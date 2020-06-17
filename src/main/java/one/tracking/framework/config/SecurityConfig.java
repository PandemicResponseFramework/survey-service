/**
 *
 */
package one.tracking.framework.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import one.tracking.framework.filter.BearerAuthenticationFilter;
import one.tracking.framework.repo.UserRepository;
import one.tracking.framework.support.JWTHelper;

/**
 * @author Marko Voß
 *
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Configuration
  @Order(0)
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
              "/swagger*/**",
              "/webjars/**",
              "/h2-console/**",
              "/v3/api-docs/**")
          .permitAll()
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
