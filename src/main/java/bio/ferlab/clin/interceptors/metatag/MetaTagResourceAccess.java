package bio.ferlab.clin.interceptors.metatag;

import bio.ferlab.clin.exceptions.RptIntrospectionException;
import bio.ferlab.clin.properties.BioProperties;
import bio.ferlab.clin.utils.Helpers;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class MetaTagResourceAccess {

  // all meta/tags compatible resources, adding one need an implementation bellow
  private static final List<String> RESOURCES_WITH_TAGS = List.of("ServiceRequest");
  private static final String TOKEN_ATTR_FHIR_ORG_ID = "fhir_organization_id";
  private static final String USER_ALL_TAGS = "*";
  
  private final BioProperties bioProperties;
  
  public MetaTagResourceAccess(BioProperties bioProperties) {
    this.bioProperties = bioProperties;
  }

  public boolean canSeeResource(RequestDetails requestDetails, IBaseResource resource) {
    if (RequestTypeEnum.GET.equals(requestDetails.getRequestType())
        && bioProperties.isTaggingEnabled()
        && isResourceWithTags(resource)) {
      final List<String> userTags = getUserTags(requestDetails);
      final List<String> resourceTags = resource.getMeta().getTag().stream().map(IBaseCoding::getCode).collect(Collectors.toList());
      return userTags.contains(USER_ALL_TAGS) || resourceTags.stream().anyMatch(userTags::contains);
    }
    return true;
  }

  public List<String> getUserTags(RequestDetails requestDetails) {
    final var bearer = requestDetails.getHeader(HttpHeaders.AUTHORIZATION);
    final var rpt = Helpers.extractAccessTokenFromBearer(bearer);
    final var jwt = JWT.decode(rpt);

    final List<String> tags = new ArrayList<>();

    final var isSystem = Optional.ofNullable(jwt.getClaim("azp")).map(Claim::asString)
        .orElse("").equals(bioProperties.getAuthSystemId());

    if (isSystem) {
      tags.add(USER_ALL_TAGS);
    } else {
      tags.add(Optional.ofNullable(jwt.getClaim(TOKEN_ATTR_FHIR_ORG_ID)).map(Claim::asString)
          .orElseThrow(() -> new RptIntrospectionException("missing " + TOKEN_ATTR_FHIR_ORG_ID)));
    }

    return tags;
  }

  public boolean isResourceWithTags(String resourceName) {
    return resourceName !=null && RESOURCES_WITH_TAGS.contains(resourceName);
  }

  public boolean isResourceWithTags(IBaseResource resource) {
    return this.isResourceWithTags(resource.getClass().getSimpleName());
  }
}
