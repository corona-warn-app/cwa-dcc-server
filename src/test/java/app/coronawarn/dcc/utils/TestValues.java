package app.coronawarn.dcc.utils;

import app.coronawarn.dcc.model.RegistrationToken;
import feign.Request;
import feign.RequestTemplate;
import java.util.HashMap;
import java.util.UUID;

public class TestValues {

  public static final String registrationTokenValue = UUID.randomUUID().toString();
  public static final byte[] encryptedDek = new byte[]{1, 2, 3, 4, 5};
  public static final byte[] encryptedDcc = new byte[]{6, 7, 8, 9, 10};
  public static final byte[] partialDcc = TestUtils.generatePartialDcc();
  public static final String dccHash = "b".repeat(64);
  public static final String testId = "d".repeat(64);
  public static final String labId = "e".repeat(64);
  public static final String partnerId = "f".repeat(64);
  public static final RegistrationToken registrationToken = new RegistrationToken(registrationTokenValue);
  public static final Request dummyRequest =
    Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());

}
