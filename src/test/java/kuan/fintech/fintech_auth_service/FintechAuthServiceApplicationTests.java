package kuan.fintech.fintech_auth_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.UUID;
import kuan.fintech.common.correlation.CorrelationContext;
import kuan.fintech.fintech_auth_service.infrastructure.persistence.repository.AuthUserJpaRepository;
import kuan.fintech.fintech_auth_service.infrastructure.persistence.repository.RefreshTokenJpaRepository;
import kuan.fintech.fintech_auth_service.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FintechAuthServiceApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthUserJpaRepository authUserRepository;

	@Autowired
	private RefreshTokenJpaRepository refreshTokenRepository;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Test
	void contextLoads() throws Exception {
		mockMvc.perform(get("/auth/ping")
						.header(CorrelationContext.CORRELATION_ID_HEADER, "test-ping-001"))
				.andExpect(status().isOk())
				.andExpect(header().string(CorrelationContext.CORRELATION_ID_HEADER, "test-ping-001"))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").value("fintech-auth-service is running"));
	}

	@Test
	void registerCreatesActiveCustomerWithHashedPassword() throws Exception {
		String email = randomEmail();
		String password = "Password@123";

		mockMvc.perform(post("/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s"}
								""".formatted(email, password)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.email").value(email))
				.andExpect(jsonPath("$.data.status").value("ACTIVE"));

		var user = authUserRepository.findByEmail(email).orElseThrow();
		assertThat(user.getPasswordHash()).isNotEqualTo(password);
		assertThat(user.getPasswordHash()).startsWith("$2");
		assertThat(user.getRoles()).extracting("name").containsExactly("CUSTOMER");
	}

	@Test
	void registerRejectsDuplicateEmail() throws Exception {
		String email = randomEmail();
		register(email, "Password@123");

		mockMvc.perform(post("/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"Password@123"}
								""".formatted(email.toUpperCase())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("AUTH_EMAIL_ALREADY_EXISTS"));
	}

	@Test
	void loginReturnsJwtAndRefreshToken() throws Exception {
		String email = randomEmail();
		register(email, "Password@123");

		MvcResult loginResult = login(email, "Password@123")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.accessToken").isString())
				.andExpect(jsonPath("$.data.refreshToken").isString())
				.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.data.expiresIn").value(900))
				.andReturn();

		String accessToken = readToken(loginResult, "accessToken");
		String refreshToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.data.refreshToken");
		Jwt jwt = jwtDecoder.decode(accessToken);

		assertThat(jwt.getClaimAsString("iss")).isEqualTo("fintech-auth-service");
		assertThat(jwt.getSubject()).isNotBlank();
		assertThat(jwt.getClaimAsString("email")).isEqualTo(email);
		assertThat(jwt.getClaimAsStringList("roles")).containsExactly("CUSTOMER");
		assertThat(jwt.getClaimAsString("scope")).isEqualTo("ROLE_CUSTOMER");
		assertThat(jwt.getClaimAsString("token_use")).isEqualTo(JwtTokenProvider.TOKEN_USE_ACCESS);

		assertThat(refreshTokenRepository.findAll())
				.anySatisfy(savedToken -> {
					assertThat(savedToken.getTokenHash()).isNotEqualTo(refreshToken);
					assertThat(savedToken.getTokenHash()).hasSize(64);
				});
	}

	@Test
	void loginRejectsInvalidPassword() throws Exception {
		String email = randomEmail();
		register(email, "Password@123");

		login(email, "WrongPassword")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("AUTH_INVALID_CREDENTIALS"));
	}

	@Test
	void refreshRotatesRefreshToken() throws Exception {
		String email = randomEmail();
		register(email, "Password@123");
		String originalRefreshToken = readToken(login(email, "Password@123").andReturn(), "refreshToken");

		MvcResult refreshResult = mockMvc.perform(post("/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":"%s"}
								""".formatted(originalRefreshToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.accessToken").isString())
				.andExpect(jsonPath("$.data.refreshToken").isString())
				.andReturn();

		String rotatedRefreshToken = readToken(refreshResult, "refreshToken");
		assertThat(rotatedRefreshToken).isNotEqualTo(originalRefreshToken);

		mockMvc.perform(post("/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":"%s"}
								""".formatted(originalRefreshToken)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("AUTH_REFRESH_TOKEN_REVOKED"));
	}

	@Test
	void logoutRevokesRefreshToken() throws Exception {
		String email = randomEmail();
		register(email, "Password@123");
		String refreshToken = readToken(login(email, "Password@123").andReturn(), "refreshToken");

		mockMvc.perform(post("/auth/logout")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":"%s"}
								""".formatted(refreshToken)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.loggedOut").value(true));

		mockMvc.perform(post("/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":"%s"}
								""".formatted(refreshToken)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("AUTH_REFRESH_TOKEN_REVOKED"));
	}

	@Test
	void meReturnsClaimsFromBearerToken() throws Exception {
		String email = randomEmail();
		register(email, "Password@123");
		String accessToken = readToken(login(email, "Password@123").andReturn(), "accessToken");

		mockMvc.perform(get("/auth/me")
						.header("Authorization", "Bearer " + accessToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.email").value(email))
				.andExpect(jsonPath("$.data.roles[0]").value("CUSTOMER"));
	}

	@Test
	void meRejectsJwtWithoutAccessTokenUseClaim() throws Exception {
		String token = signedJwtWithoutTokenUse();

		mockMvc.perform(get("/auth/me")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	private void register(String email, String password) throws Exception {
		mockMvc.perform(post("/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s"}
								""".formatted(email, password)))
				.andExpect(status().isOk());
	}

	private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
		return mockMvc.perform(post("/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","password":"%s"}
						""".formatted(email, password)));
	}

	private String readToken(MvcResult result, String tokenName) throws Exception {
		return JsonPath.read(result.getResponse().getContentAsString(), "$.data." + tokenName);
	}

	private String signedJwtWithoutTokenUse() {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("fintech-auth-service")
				.issuedAt(now)
				.expiresAt(now.plusSeconds(900))
				.subject(UUID.randomUUID().toString())
				.claim("email", "customer@example.com")
				.claim("roles", java.util.List.of("CUSTOMER"))
				.build();

		return jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(),
				claims)).getTokenValue();
	}

	private String randomEmail() {
		return "customer-%s@example.com".formatted(UUID.randomUUID());
	}

}
