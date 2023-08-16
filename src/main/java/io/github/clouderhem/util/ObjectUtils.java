package io.github.clouderhem.util;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

/**
 * @author Aaron Yeung
 * @date 8/16/2023 12:53 PM
 */
public class ObjectUtils {

    /**
     * 查看该field是否有getter方法
     *
     * @param typeElement
     * @param fieldName
     * @return
     */
    public static boolean hasGetter(TypeElement typeElement, String fieldName) {
        String getter = buildGetterName(fieldName);
        for (ExecutableElement executableElement : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
            if (!executableElement.getModifiers().contains(Modifier.STATIC)
                    && executableElement.getSimpleName().toString().equals(getter)
                    && executableElement.getParameters().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    /**
     * 生成getter方法名称
     *
     * @param fieldName
     * @return
     */
    public static String buildGetterName(String fieldName) {
        return String.format("get%s%s", fieldName.substring(0, 1).toUpperCase(), fieldName.substring(1).toLowerCase());
    }
}
