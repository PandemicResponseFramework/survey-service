/**
 *
 */
package one.tracking.framework.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import one.tracking.framework.repo.UserRepository;
import one.tracking.framework.security.BearerAuthenticationFilter;
import one.tracking.framework.util.JWTHelper;

/**
 * @author Marko Voß
 *
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Configuration
  @Profile("dev")
  @Order(0)
  public class SecurityConfigDev extends WebSecurityConfigurerAdapter {

    @Value("${app.dev.user}")
    private String username;

    @Value("${app.dev.password}")
    private String password;

    @Bean
    public PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
      final BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
      entryPoint.setRealmName("default");
      return entryPoint;
    }

    @Autowired
    public void configureGlobal(final AuthenticationManagerBuilder auth) throws Exception {
      auth.inMemoryAuthentication()
          .withUser(this.username)
          .password(passwordEncoder().encode(this.password))
          .authorities("ROLE_ADMIN");
    }

    @Override
    protected void configure(final HttpSecurity http) throws Exception {

      http.antMatcher("/auth/register/**")
          .csrf().disable()
          .authorizeRequests()
          .anyRequest().authenticated()
          .and()
          .httpBasic()
          .authenticationEntryPoint(authenticationEntryPoint())
          .and()
          .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }
  }

  @Configuration
  @Order(1)
  public class StandardSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private JWTHelper jwtHelper;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
      http.cors().and().csrf().disable()
          .authorizeRequests()
          .antMatchers(
              "/v2/api-docs",
              "/configuration/**",
              "/swagger*/**",
              "/webjars/**",
              "/h2-console/**",
              "/v3/api-docs/**")
          .permitAll()
          .antMatchers("/auth/verify").permitAll()
          .anyRequest().authenticated()
          .and()
          .addFilter(bearerAuthenticationFilter())
          .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
      http.headers().frameOptions().disable();
    }

    public BearerAuthenticationFilter bearerAuthenticationFilter() throws Exception {

      return new BearerAuthenticationFilter(authenticationManager(), this.jwtHelper) {

        @Override
        protected boolean checkIfUserExists(final String userId) {
          return StandardSecurityConfig.this.userRepository.existsById(userId);
        }

      };
    }
  }
}
