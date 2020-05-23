/**
 *
 */
package one.tracking.framework.integration;

import java.util.Arrays;
import java.util.List;
import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import lombok.Builder;
import lombok.Data;

/**
 * @author Marko Voß
 *
 */
@Data
@Builder
public class MockBeanInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

  private List<Class<?>> mockedBeans;

  @Override
  public void initialize(final ConfigurableApplicationContext applicationContext) {

    applicationContext.addBeanFactoryPostProcessor(
        beanFactory -> beanFactory.addBeanPostProcessor(new BeanPostProcessor() {

          @Override
          public Object postProcessBeforeInitialization(final Object bean, final String beanName)
              throws BeansException {

            return MockBeanInitializer.this.mockedBeans.contains(bean.getClass())
                ? Mockito.mock(bean.getClass())
                : bean;
          }

        }));
  }

  public static class MockBeanInitializerBuilder {

    private List<Class<?>> mockedBeans;

    public MockBeanInitializerBuilder mockedBeans(final Class<?>... beans) {
      this.mockedBeans = Arrays.asList(beans);
      return this;
    }
  }
}
