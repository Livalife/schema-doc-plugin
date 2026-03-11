package com.livalife.schemadoc;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

/**
 * Provides Quick Documentation from {@code @Schema(description = "...")} annotations
 * when the element has no Javadoc comment.
 */
public class SchemaDocumentationProvider extends AbstractDocumentationProvider {

    private static final String SCHEMA_FQN = "io.swagger.v3.oas.annotations.media.Schema";

    @Override
    public @Nullable @Nls String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        if (!(element instanceof PsiModifierListOwner owner)) {
            return null;
        }

        if (owner instanceof PsiDocCommentOwner docOwner && docOwner.getDocComment() != null) {
            return null;
        }

        PsiAnnotation schema = owner.getAnnotation(SCHEMA_FQN);
        if (schema == null) {
            return null;
        }

        String description = resolveStringAttribute(schema, "description");
        if (description == null || description.isBlank()) {
            return null;
        }

        return renderHtml(owner, schema, description);
    }

    private @Nullable String resolveStringAttribute(PsiAnnotation annotation, String name) {
        PsiAnnotationMemberValue value = annotation.findAttributeValue(name);
        if (value == null) {
            return null;
        }
        if (value instanceof PsiLiteralExpression literal && literal.getValue() instanceof String s) {
            return s;
        }
        Object constant = JavaPsiFacade.getInstance(annotation.getProject())
                .getConstantEvaluationHelper()
                .computeConstantExpression(value);
        return constant instanceof String s ? s : null;
    }

    private String renderHtml(PsiModifierListOwner owner, PsiAnnotation schema, String description) {
        var sb = new StringBuilder();

        sb.append("<div class='definition'><pre>");
        appendSignature(sb, owner);
        sb.append("</pre></div>");

        sb.append("<div class='content'>");
        sb.append("<p>").append(escapeHtml(description)).append("</p>");

        String example = resolveStringAttribute(schema, "example");
        if (example != null && !example.isBlank()) {
            sb.append("<p><b>Example:</b> <code>").append(escapeHtml(example)).append("</code></p>");
        }

        sb.append("<br/><p><i>From <code>@Schema</code> annotation</i></p>");
        sb.append("</div>");

        return sb.toString();
    }

    private void appendSignature(StringBuilder sb, PsiModifierListOwner owner) {
        switch (owner) {
            case PsiClass cls -> {
                sb.append(cls.isInterface() ? "interface " : cls.isEnum() ? "enum " : "class ");
                String qName = cls.getQualifiedName();
                sb.append(escapeHtml(qName != null ? qName : cls.getName()));
            }
            case PsiField field -> {
                sb.append(escapeHtml(field.getType().getPresentableText()));
                sb.append(" ");
                sb.append(escapeHtml(field.getName()));
            }
            case PsiMethod method -> {
                PsiType returnType = method.getReturnType();
                if (returnType != null) {
                    sb.append(escapeHtml(returnType.getPresentableText())).append(" ");
                }
                sb.append(escapeHtml(method.getName()));
                sb.append("(");
                PsiParameter[] params = method.getParameterList().getParameters();
                for (int i = 0; i < params.length; i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(escapeHtml(params[i].getType().getPresentableText()));
                    sb.append(" ").append(escapeHtml(params[i].getName()));
                }
                sb.append(")");
            }
            case PsiParameter param -> {
                sb.append(escapeHtml(param.getType().getPresentableText()));
                sb.append(" ");
                sb.append(escapeHtml(param.getName()));
            }
            default -> {}
        }
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
