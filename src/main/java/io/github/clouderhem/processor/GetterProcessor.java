package io.github.clouderhem.processor;


import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.*;
import io.github.clouderhem.annotation.Getter;
import io.github.clouderhem.util.ObjectUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
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

class GetterProcessorTreeTranslator extends TreeTranslator {

    private final Messager messager;

    private final TreeMaker treeMaker;

    private final Names names;

    private final java.util.List<String> fieldNameListWithGetter;

    public GetterProcessorTreeTranslator(ProcessingEnvironment processingEnvironment,
                                         java.util.List<String> fieldNameListWithGetter) {
        this.messager = processingEnvironment.getMessager();

        Context context = ((JavacProcessingEnvironment) processingEnvironment).getContext();
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
        this.fieldNameListWithGetter = fieldNameListWithGetter;
    }

    @Override
    public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
        java.util.List<JCTree.JCVariableDecl> jcVariableDeclList = new ArrayList<>();

        // 获取VARIABLE
        jcClassDecl.defs.stream()
                .filter(jcTree -> jcTree.getKind().equals(Tree.Kind.VARIABLE))
                .forEach(jcTree -> jcVariableDeclList.add((JCTree.JCVariableDecl) jcTree));

        jcVariableDeclList.stream()
                .filter(jcVariableDecl -> !fieldNameListWithGetter.contains(jcVariableDecl.getName().toString())).
                forEach(jcVariableDecl -> {
                    messager.printMessage(Diagnostic.Kind.NOTE, String.format("Created a getter for [ %s.%s ]",
                            jcClassDecl.getSimpleName(), jcVariableDecl.getName()));

                    treeMaker.pos = jcVariableDecl.pos;
                    jcClassDecl.defs = jcClassDecl.defs.append(createJCMethodDecl(jcVariableDecl));
                });

        super.visitClassDef(jcClassDecl);
    }

    private JCTree.JCMethodDecl createJCMethodDecl(JCTree.JCVariableDecl jcVariableDecl) {
        ListBuffer<JCTree.JCStatement> statementList = new ListBuffer<>();

        JCTree.JCReturn jcReturn = treeMaker.Return(treeMaker.Select(treeMaker.Ident(names.fromString("this")),
                jcVariableDecl.getName()));
        statementList.add(jcReturn);

        JCTree.JCBlock block = treeMaker.Block(0L, statementList.toList());

        return treeMaker.MethodDef(treeMaker.Modifiers(Flags.PUBLIC),
                buildGetterMethodName(jcVariableDecl.getName()), jcVariableDecl.vartype, List.nil(), List.nil(),
                List.nil(), block, null);

    }

    private Name buildGetterMethodName(Name fieldName) {
        return names.fromString(ObjectUtils.buildGetterName(fieldName.toString()));
    }
}
