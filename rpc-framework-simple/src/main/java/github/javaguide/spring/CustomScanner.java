package github.javaguide.spring;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.annotation.Annotation;

/**
 * custom package scanner
 * 自定义扫描器，继承自ClassPathBeanDefinitionScanner
 *
 * @author shuang.kou
 * @createTime 2020年08月10日 21:42:00
 */
public class CustomScanner extends ClassPathBeanDefinitionScanner {

    /**
     * 构造方法
     *
     * @param registry Bean定义注册表
     * @param annoType 需要扫描的注解类型
     */
    public CustomScanner(BeanDefinitionRegistry registry, Class<? extends Annotation> annoType) {
        super(registry);
        // 添加包含的过滤器，只包含指定注解类型的Bean
        super.addIncludeFilter(new AnnotationTypeFilter(annoType));
    }

    /**
     * 扫描指定的包，结合上面的方法，把扫描包里面带有特定注解的类注册成bean
     *
     * @param basePackages 需要扫描的包名
     * @return 扫描到的Bean的数量
     */
    @Override
    public int scan(String... basePackages) {
        return super.scan(basePackages);
    }
}
