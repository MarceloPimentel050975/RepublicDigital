package br.com.digitalRepublic.util.db;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Version;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Classe para auxiliar na obten��o de informa��es por reflex�o de objetos,
 * especialmente classes persistentes (@Entity).
 *
 * @param <T>
 *                Classe tratada (ver construtor
 *                {@link #ReflectionHelper(Object)}).
 */
public final class ReflectionHelper<T> {

    private final class Property {
	private final String name;
	private final List<Annotation> annotations;
	private final Class<?> type;
	
	public Property(String name, Class<?> type, Annotation[] annotations) {
	    this.name = name;
	    this.type = type;
	    this.annotations = Collections.unmodifiableList(Arrays.asList(annotations));
	}
	
	public String getName() {
	    return name;
	}
	
	public List<Annotation> getAnnotations() {
	    return annotations;
	}
	
	public Class<?> getType() {
	    return type;
	}
	
    }
    
    private static final Log LOG = LogFactory.getLog(ReflectionHelper.class);
    private static final Pattern ACCESSOR_PATTERN = Pattern
            .compile("(?:get|is)([A-Z])(.*+)");
//    private static final Pattern MODIFIER_PATTERN = Pattern
//            .compile("set([A-Z])(.*+)");
    private Set<Property> properties;

    private T instance;
    private Class<?> targetClass;

    /**
     * Cria uma inst�ncia para lidar com as propriedades de <code>obj</code>.
     *
     * @param obj
     *                A inst�ncia a ser tratada.
     */
    public ReflectionHelper(T obj) {
        this.instance = obj;
        discoverProperties();
    }

    /**
     * Cria uma c�pia total do objeto tratado. Se for uma classe persistente, os
     * relacionamentos recebem o seguinte tratamento:
     * <ul>
     * <li>@Id: Propriedade � anulada.</li>
     * <li>@ManyToMany: Cole��o � duplicada mantendo refer�ncias originais.</li>
     * <li>@OneToMany: Cole��o � duplicada, assim como refer�ncias.</li>
     * <li>@OneToOne: Propriedade � anulada.</li>
     * </ul>
     * Desta forma o objeto retornado estar� (teoricamente) pronto para ser
     * persistido como c�pia do original.
     *
     * @return Nova c�pia do objeto tratado por esta inst�ncia.
     */
    public T deepCopy() {
        return deepCopy(new HashMap<String, Object>());
    }

    @SuppressWarnings("unchecked")
    private T deepCopy(Map<String, Object> parentReference) {
        try {
            Map<String, Object> currentReference = new HashMap<String, Object>();
            T copy = shallowCopy();
            String idProperty = getIdProperty();
            PropertyUtils.setProperty(copy, idProperty, null);
            for (Property property : properties) {
                ManyToMany manyToMany = getAnnotation(property,
                        ManyToMany.class);
                OneToMany oneToMany = getAnnotation(property, OneToMany.class);
                if (manyToMany != null || oneToMany != null) {
                    Collection<Object> collection = (Collection<Object>) PropertyUtils
                            .getProperty(instance, property.getName());
                    Collection<Object> collectionCopy = null;
                    if (collection instanceof List) {
                        collectionCopy = new ArrayList<Object>(collection);
                    } else if (collection instanceof Set) {
                        collectionCopy = new LinkedHashSet<Object>(collection);
                    }
                    PropertyUtils.setProperty(copy, property.getName(), collectionCopy);
                    if (oneToMany != null && collectionCopy != null) {
                        String inverse = oneToMany.mappedBy();
                        if (inverse != null && !"".equals(inverse)) {
                            currentReference.put(inverse, copy);
                        }
                        collectionCopy.clear();
                        for (Object obj : collection) {
                            ReflectionHelper<Object> helper = new ReflectionHelper<Object>(
                                    obj);
                            collectionCopy.add(helper
                                    .deepCopy(currentReference));
                        }
                    }
                    continue;
                }
                OneToOne oneToOne = getAnnotation(property, OneToOne.class);
                if (oneToOne != null) {
                    /*
                     * @OneToOne significa que � uma composi��o. Presumimos que
                     * o mappedBy estar� preenchido no lado "pai", e o lado
                     * "filho" (que cont�m a FK) est� do outro lado. Assim,
                     * sendo, supomos que o "pai" deve ser removido e o "filho"
                     * copiado recursivamente.
                     */
                    Object value = PropertyUtils.getProperty(copy, property.getName());
                    if (value != null && oneToOne.mappedBy() != null
                            && !"".equals(oneToOne.mappedBy())) {
                        currentReference.put(oneToOne.mappedBy(), copy);
                        ReflectionHelper<Object> helper = new ReflectionHelper<Object>(value);
                        Object nestedCopy = helper.deepCopy(currentReference);
                        PropertyUtils.setProperty(copy, property.getName(), nestedCopy);
                    }
                    else {
                        /* FIXME usar mesmo estilo de c�pia bidirecional que os *ToMany */
                        PropertyUtils.setProperty(copy, property.getName(), null);
                    }
                }
                ManyToOne manyToOne = getAnnotation(property, ManyToOne.class);
                if (manyToOne != null) {
                    Object cachedCopy = parentReference.get(property.getName());
                    if (cachedCopy != null) {
                        PropertyUtils.setProperty(copy, property.getName(), cachedCopy);
                    }
                }
            }
            return copy;
        } catch (Exception e) {
            LOG.error(null, e);
        }
        return null;
    }

    public String getIdProperty() {
        for (Property property : properties) {
            Id id = getAnnotation(property, Id.class);
            if (id != null) {
                return property.getName();
            }
            EmbeddedId embedded = getAnnotation(property, EmbeddedId.class);
            if (embedded != null) {
                return property.getName();
            }
        }
        return null;
    }
    
    public boolean isEmbeddedId() {
	for (Property property : properties) {
            Id id = getAnnotation(property, Id.class);
            if (id != null) {
                return false;
            }
            EmbeddedId embedded = getAnnotation(property, EmbeddedId.class);
            if (embedded != null) {
                return true;
            }
        }
        return false;
    }

    public Serializable getId() {
        String property = getIdProperty();
        if (property == null) {
            property = getIdProperty();
        }
        try {
            return (Serializable)PropertyUtils.getProperty(instance, property);
        } catch (Exception e) {
            LOG.error("Error ", e);
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    private <A extends Annotation> A getAnnotation(Property property, Class<A> type) {
	for (Annotation annotation : property.getAnnotations()) {
	    if (type.isAssignableFrom(annotation.getClass())) {
		return (A)annotation;
	    }
	}
	return null;
    }
    
    private Annotation[] findAnnotations(String property) {
	List<Annotation> result = new ArrayList<Annotation>();
	Class<?> clazz = targetClass;
	Field field = null;
	while (clazz != Object.class && clazz != null && field == null) {
	    try {
		field = targetClass.getDeclaredField(property);
	    } catch (Exception e) {
		field = null;
		clazz = clazz.getSuperclass();
		continue;
	    }
	}
	if (field != null) {
	    Annotation[] fieldAnnotations = field.getAnnotations();
	    for (Annotation annotation : fieldAnnotations) {
		result.add(annotation);
	    }
	}
	
	Method method = null;
	clazz = targetClass;
	String getter = "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
	while (clazz != Object.class && clazz != null && method == null) {
	    try {
		method = targetClass.getDeclaredMethod(getter);
	    } catch (Exception e) {
		method = null;
		clazz = clazz.getSuperclass();
		continue;
	    }
	}
	if (method != null) {
	    Annotation[] methodAnnotations = method.getAnnotations();
	    for (Annotation annotation : methodAnnotations) {
		result.add(annotation);
	    }
	}
	
	method = null;
	clazz = targetClass;
	String setter = "set" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
	while (clazz != Object.class && clazz != null && method == null) {
	    try {
		method = targetClass.getDeclaredMethod(setter);
	    } catch (Exception e) {
		method = null;
		clazz = clazz.getSuperclass();
		continue;
	    }
	}
	if (method != null) {
	    Annotation[] methodAnnotations = method.getAnnotations();
	    for (Annotation annotation : methodAnnotations) {
		result.add(annotation);
	    }
	}
	
	return result.toArray(new Annotation[0]);
    }

    private void discoverProperties() {
	properties = new LinkedHashSet<Property>();
        try {
            targetClass = instance.getClass();
            while (targetClass.getName().indexOf("$$EnhancerByCGLIB$$") > -1) {
                targetClass = targetClass.getSuperclass();
            }
            Map<String,Class<?>> methodProps = new LinkedHashMap<String,Class<?>>();
            for (Method method : targetClass.getMethods()) {
                String methodProperty;
                Matcher matcher = ACCESSOR_PATTERN.matcher(method.getName());
                if (!matcher.matches() || method.getParameterTypes().length > 1) {
                    // no accessor, no property
                    continue;
                }
                methodProperty = matcher.group(1).toLowerCase() + matcher.group(2);
                methodProps.put(methodProperty, method.getReturnType());
                
                if (PropertyUtils.isWriteable(instance, methodProperty)) {
                    properties.add(new Property(methodProperty, method.getReturnType(), findAnnotations(methodProperty)));
                    
                }
            }
        } catch (Exception e) {
            LOG.error(null, e);
            return;
        }
    }

    /**
     * Cria uma c�pia simples do objeto tratado (n�o duplica objetos sen�o o
     * original).
     *
     * @return C�pia criada.
     */
    @SuppressWarnings("unchecked")
    public T shallowCopy() {
        try {
            T copy = (T) BeanUtils.cloneBean(instance);
            return copy;
        } catch (Exception e) {
            LOG.error("Unable to copy " + instance, e);
            return null;
        }
    }

    public boolean isEmbeddable() {
        boolean result = false;
        for (Property property : properties) {
            if (getAnnotation(property, Embeddable.class) != null) {
                result = true;
                break;
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public List<String> toOneRelationships() {
        List<String> result = new LinkedList<String>();
        for (Property property : properties) {
            if (hasAnnotation(property, OneToOne.class, ManyToOne.class)) {
                result.add(property.getName());
            }
        }
        return result;
    }
    
    private boolean hasAnnotation(Property property, Class<? extends Annotation>... annotationType) {
	for (Annotation annotation : property.getAnnotations()) {
	    if (annotation.getClass().equals(annotationType)) {
		return true;
	    }
	}
	return false;
    }

    private boolean hasAnnotation(String property, Class<? extends Annotation>... annotation) {
        return hasAnnotation(findProperty(property), annotation);
    }
    
    private Property findProperty(String name) {
	for (Property property : properties) {
	    if (property.getName().equals(name)) {
		return property;
	    }
	}
	return null;
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }

    public Set<String> getPropertyNames() {
	Set<String> result = new LinkedHashSet<String>();
	for (Property property : properties) {
	    result.add(property.getName());
	}
    	return result;
    }

    @SuppressWarnings("unchecked")
    public boolean isVersion(String property) {
	return hasAnnotation(property, Version.class);
    }

    public Class<?> getType(String property) {
	return findProperty(property).getType();
    }

}
