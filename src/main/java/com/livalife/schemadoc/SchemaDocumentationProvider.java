package com.livalife.schemadoc;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.lang.java.JavaDocumentationProvider;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
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
 * Appends {@code @Schema(description = "...")} annotation content
 * to the default Quick Documentation popup.
 */
public class SchemaDocumentationProvider extends AbstractDocumentationProvider {

    private static final String SCHEMA_FQN = "io.swagger.v3.oas.annotations.media.Schema";

    @Override
    public @Nullable @Nls String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        if (!(element instanceof PsiModifierListOwner owner)) {
            return null;
        }

        PsiAnnotation schema = findSchemaAnnotation(owner);
        if (schema == null) {
            return null;
        }

        String description = resolveStringAttribute(schema, "description");
        if (description == null || description.isBlank()) {
            return null;
        }

        String schemaSection = renderSchemaSection(schema, description);
        String baseDoc = new JavaDocumentationProvider().generateDoc(element, originalElement);

        if (baseDoc != null) {
            return baseDoc + schemaSection;
        }

        return renderStandalone(owner, schemaSection);
    }

    private @Nullable PsiAnnotation findSchemaAnnotation(PsiModifierListOwner owner) {
        PsiAnnotation schema = owner.getAnnotation(SCHEMA_FQN);
        if (schema != null) {
            return schema;
        }
        if (owner instanceof PsiMethod method) {
            PsiField field = findBackingField(method);
            if (field != null) {
                return field.getAnnotation(SCHEMA_FQN);
            }
        }
        return null;
    }

    private @Nullable PsiField findBackingField(PsiMethod method) {
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            return null;
        }
        String fieldName = extractFieldName(method);
        if (fieldName == null) {
            return null;
        }
        return containingClass.findFieldByName(fieldName, false);
    }

    private @Nullable String extractFieldName(PsiMethod method) {
        String name = method.getName();
        int paramCount = method.getParameterList().getParametersCount();
        String prefix = null;
        if (paramCount == 0 && name.startsWith("get") && name.length() > 3) {
            prefix = "get";
        } else if (paramCount == 0 && name.startsWith("is") && name.length() > 2) {
            prefix = "is";
        } else if (paramCount == 1 && name.startsWith("set") && name.length() > 3) {
            prefix = "set";
        }
        if (prefix == null) {
            return null;
        }
        String rest = name.substring(prefix.length());
        return Character.toLowerCase(rest.charAt(0)) + rest.substring(1);
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

    private String renderSchemaSection(PsiAnnotation schema, String description) {
        var sb = new StringBuilder();
        sb.append("<div class='content'>");
        sb.append("<hr/>");
        sb.append("<p><b>@Schema:</b> ").append(escapeHtml(description)).append("</p>");

        String example = resolveStringAttribute(schema, "example");
        if (example != null && !example.isBlank()) {
            sb.append("<p><b>Example:</b> <code>").append(escapeHtml(example)).append("</code></p>");
        }

        sb.append("</div>");
        return sb.toString();
    }

    private String renderStandalone(PsiModifierListOwner owner, String schemaSection) {
        var sb = new StringBuilder();
        sb.append("<div class='definition'><pre>");
        appendSignature(sb, owner);
        sb.append("</pre></div>");
        sb.append(schemaSection);
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
