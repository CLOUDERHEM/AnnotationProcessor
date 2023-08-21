package io.github.clouderhem.annotationprocessor.visitor.translator;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import io.github.clouderhem.annotationprocessor.util.ObjectUtils;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;
import java.util.ArrayList;

/**
 * @author Aaron Yeung
 * @date 8/21/2023 6:53 PM
 */
public class GetterProcessorTreeTranslator extends TreeTranslator {

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
                    messager.printMessage(Diagnostic.Kind.NOTE, String.format("Created a getter " +
                                    "for [ %s.%s ]",
                            jcClassDecl.getSimpleName(), jcVariableDecl.getName()));

                    treeMaker.pos = jcVariableDecl.pos;
                    jcClassDecl.defs = jcClassDecl.defs.append(createJCMethodDecl(jcVariableDecl));
                });

        super.visitClassDef(jcClassDecl);
    }

    private JCTree.JCMethodDecl createJCMethodDecl(JCTree.JCVariableDecl jcVariableDecl) {
        ListBuffer<JCTree.JCStatement> statementList = new ListBuffer<>();

        JCTree.JCReturn jcReturn =
                treeMaker.Return(treeMaker.Select(treeMaker.Ident(names.fromString("this")),
                        jcVariableDecl.getName()));
        statementList.add(jcReturn);

        JCTree.JCBlock block = treeMaker.Block(0L, statementList.toList());

        return treeMaker.MethodDef(treeMaker.Modifiers(Flags.PUBLIC),
                buildGetterMethodName(jcVariableDecl.getName()), jcVariableDecl.vartype,
                List.nil(), List.nil(),
                List.nil(), block, null);

    }

    private Name buildGetterMethodName(Name fieldName) {
        return names.fromString(ObjectUtils.buildGetterName(fieldName.toString()));
    }
}
