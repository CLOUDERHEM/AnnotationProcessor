package io.github.clouderhem.annotationprocessor.processor;


import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.*;
import io.github.clouderhem.annotationprocessor.annotation.Getter;
import io.github.clouderhem.annotationprocessor.util.ObjectUtils;
import io.github.clouderhem.annotationprocessor.visitor.translator.GetterProcessorTreeTranslator;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Aaron Yeung
 * @date 8/15/2023 7:37 PM
 */
@SupportedAnnotationTypes("io.github.clouderhem.annotation.Getter")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class GetterProcessor extends AbstractProcessor {

    private JavacTrees javacTrees;

    private ProcessingEnvironment processingEnvironment;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.javacTrees = JavacTrees.instance(processingEnv);
        this.processingEnvironment = processingEnv;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        java.util.List<Pair<TypeElement, JCTree.JCClassDecl>> jcClassDeclList =
                ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(Getter.class)).stream()
                        .map(typeElement -> new Pair<>(typeElement, javacTrees.getTree(typeElement))).collect(Collectors.toList());

        java.util.List<String> fieldNameListWithGetter = new ArrayList<>();
        GetterProcessorTreeTranslator getterProcessorTreeTranslator =
                new GetterProcessorTreeTranslator(processingEnvironment,
                        fieldNameListWithGetter);

        for (Pair<TypeElement, JCTree.JCClassDecl> typeElementJCClassDeclPair : jcClassDeclList) {
            TypeElement typeElement = typeElementJCClassDeclPair.fst;
            fieldNameListWithGetter.addAll(getFieldNameListWithGetter(typeElement));

            typeElementJCClassDeclPair.snd.accept(getterProcessorTreeTranslator);
        }

        return true;
    }

    private java.util.List<String> getFieldNameListWithGetter(TypeElement typeElement) {
        return ElementFilter.fieldsIn(typeElement.getEnclosedElements()).stream()
                .filter(variableElement -> ObjectUtils.hasGetter(typeElement,
                        variableElement.getSimpleName().toString()))
                .map(variableElement -> variableElement.getSimpleName().toString()).collect(Collectors.toList());
    }
}

