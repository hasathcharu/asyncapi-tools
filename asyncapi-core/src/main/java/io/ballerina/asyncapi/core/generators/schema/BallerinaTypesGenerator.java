/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com). All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.asyncapi.core.generators.schema;

import io.apicurio.datamodels.models.Schema;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25ComponentsImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25DocumentImpl;
import io.apicurio.datamodels.models.asyncapi.v25.AsyncApi25SchemaImpl;
import io.ballerina.asyncapi.core.GeneratorConstants;
import io.ballerina.asyncapi.core.GeneratorUtils;
import io.ballerina.asyncapi.core.exception.BallerinaAsyncApiException;
import io.ballerina.asyncapi.core.generators.schema.ballerinatypegenerators.AllOfRecordTypeGenerator;
import io.ballerina.asyncapi.core.generators.schema.ballerinatypegenerators.ArrayTypeGenerator;
import io.ballerina.asyncapi.core.generators.schema.ballerinatypegenerators.RecordTypeGenerator;
import io.ballerina.asyncapi.core.generators.schema.ballerinatypegenerators.TypeGenerator;
import io.ballerina.asyncapi.core.generators.schema.ballerinatypegenerators.UnionTypeGenerator;
import io.ballerina.asyncapi.core.generators.schema.model.GeneratorMetaData;
import io.ballerina.compiler.syntax.tree.AbstractNodeFactory;
import io.ballerina.compiler.syntax.tree.IdentifierToken;
import io.ballerina.compiler.syntax.tree.ImportDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModuleMemberDeclarationNode;
import io.ballerina.compiler.syntax.tree.ModulePartNode;
import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeFactory;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.compiler.syntax.tree.RecordTypeDescriptorNode;
import io.ballerina.compiler.syntax.tree.SyntaxTree;
import io.ballerina.compiler.syntax.tree.Token;
import io.ballerina.compiler.syntax.tree.TypeDefinitionNode;
import io.ballerina.tools.text.TextDocument;
import io.ballerina.tools.text.TextDocuments;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static io.ballerina.asyncapi.core.GeneratorConstants.CONNECTION_CONFIG;
import static io.ballerina.asyncapi.core.GeneratorConstants.HTTP;
import static io.ballerina.asyncapi.core.GeneratorConstants.OBJECT;
import static io.ballerina.asyncapi.core.GeneratorConstants.RESPONSE_MESSAGE;
import static io.ballerina.asyncapi.core.GeneratorConstants.RESPONSE_MESSAGE_WITH_ID_VAR_NAME;
import static io.ballerina.asyncapi.core.GeneratorConstants.STRING;
import static io.ballerina.asyncapi.core.GeneratorConstants.WEBSOCKET;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createEmptyNodeList;
import static io.ballerina.compiler.syntax.tree.AbstractNodeFactory.createNodeList;

/**
 * This class wraps the {@link Schema} from openapi models inorder to overcome complications
 * while populating syntax tree.
 *
 * @since 1.3.0
 */
public class BallerinaTypesGenerator {

    private final List<TypeDefinitionNode> typeDefinitionNodeList;
    private final boolean hasConstraints;


    /**
     * This public constructor is used to generate record and other relevant data type when the nullable flag is
     * enabled in the asyncAPI command.
     *
     * @param asyncAPI               AsyncAPI definition
     * @param typeDefinitionNodeList list of types generated by earlier generations
     */
    public BallerinaTypesGenerator(AsyncApi25DocumentImpl asyncAPI,
                                   List<TypeDefinitionNode> typeDefinitionNodeList) {
        GeneratorMetaData.createInstance(asyncAPI, false);
        this.typeDefinitionNodeList = typeDefinitionNodeList;
        this.hasConstraints = false;
    }

    /**
     * This public constructor is used to generate record and other relevant data type when the nullable flag is
     * enabled in the asyncAPI command.
     *
     * @param asyncAPI OAS definition
     */
    public BallerinaTypesGenerator(AsyncApi25DocumentImpl asyncAPI) {
        this(asyncAPI, new LinkedList<>());
    }

//    /**
//     * This public constructor is used to generate record and other relevant data type when the absent of the
//     nullable
//     * flag in the openapi command.
//     *
//     * @param asyncAPI OAS definition
//     */
//    public BallerinaTypesGenerator(AsyncApi25DocumentImpl asyncAPI) {
//        this(asyncAPI,  new LinkedList<>());
//    }

    /**
     * This public constructor is used to generate record and other relevant data type when the nullable flag is
     * enabled in the openapi command.
     *
     * @param asyncAPI               OAS definition
     * @param typeDefinitionNodeList list of types generated by earlier generations
     * @param generateServiceType    indicate whether the service generation includes service type
     */
    public BallerinaTypesGenerator(AsyncApi25DocumentImpl asyncAPI,
                                   List<TypeDefinitionNode> typeDefinitionNodeList,
                                   boolean generateServiceType) {
        GeneratorMetaData.createInstance(asyncAPI, generateServiceType);
        this.typeDefinitionNodeList = typeDefinitionNodeList;
        this.hasConstraints = false;
    }


    /**
     * Create Type Definition Node for a given OpenAPI schema.
     *
     * @param schema   AsyncAPI schema
     * @param typeName IdentifierToken of the name of the type
     * @return {@link TypeDefinitionNode}
     * @throws BallerinaAsyncApiException when unsupported schema type is found
     */
    public TypeDefinitionNode getTypeDefinitionNode(AsyncApi25SchemaImpl schema, String typeName,
                                                           List<Node> schemaDocs)
            throws BallerinaAsyncApiException {
        IdentifierToken typeNameToken = AbstractNodeFactory.createIdentifierToken(GeneratorUtils.getValidName(
                typeName.trim(), true));
        TypeGenerator typeGenerator = TypeGeneratorUtils.getTypeGenerator(schema, GeneratorUtils.getValidName(
                typeName.trim(), true), null);
//        List<AnnotationNode> typeAnnotations = new ArrayList<>();
//        AnnotationNode constraintNode = TypeGeneratorUtils.generateConstraintNode(schema);
//        if (constraintNode != null) {
//            typeAnnotations.add(constraintNode);
//        }
        TypeGeneratorUtils.getRecordDocs(schemaDocs, schema);
        TypeDefinitionNode typeDefinitionNode =
                typeGenerator.generateTypeDefinitionNode(typeNameToken, schemaDocs);
        if (typeGenerator instanceof ArrayTypeGenerator &&
                !typeGenerator.getTypeDefinitionNodeList().isEmpty()) {
            typeDefinitionNodeList.addAll(typeGenerator.getTypeDefinitionNodeList());
        } else if (typeGenerator instanceof RecordTypeGenerator &&
                !typeGenerator.getTypeDefinitionNodeList().isEmpty()) {
            removeDuplicateNode(typeGenerator.getTypeDefinitionNodeList());
        } else if (typeGenerator instanceof AllOfRecordTypeGenerator &&
                !typeGenerator.getTypeDefinitionNodeList().isEmpty()) {
            removeDuplicateNode(typeGenerator.getTypeDefinitionNodeList());
        } else if (typeGenerator instanceof UnionTypeGenerator &&
                !typeGenerator.getTypeDefinitionNodeList().isEmpty()) {
            removeDuplicateNode(typeGenerator.getTypeDefinitionNodeList());
        }
        return typeDefinitionNode;
    }

    /**
     * Remove duplicate of the TypeDefinitionNode.
     */
    private void removeDuplicateNode(List<TypeDefinitionNode> newConstraintNode) {

        for (TypeDefinitionNode newNode : newConstraintNode) {
            boolean isExist = false;
            for (TypeDefinitionNode oldNode : typeDefinitionNodeList) {
                if (newNode.typeName().text().equals(oldNode.typeName().text())) {
                    isExist = true;
                    break;
                }
            }
            if (!isExist) {
                typeDefinitionNodeList.add(newNode);
            }
        }
    }

    /**
     * Generate syntaxTree for component schema.
     */
    public SyntaxTree generateSyntaxTree() throws BallerinaAsyncApiException {
        AsyncApi25DocumentImpl asyncAPI = GeneratorMetaData.getInstance().getAsyncAPI();
        List<TypeDefinitionNode> typeDefinitionNodeListForSchema = new ArrayList<>();
        if (asyncAPI.getComponents() != null) {
            // Create typeDefinitionNode
            AsyncApi25ComponentsImpl components = (AsyncApi25ComponentsImpl) asyncAPI.getComponents();
            Map<String, Schema> schemas = components.getSchemas();

            if (schemas != null) {
//                createResponseMessageRecord(schemas);
//                if(idMethodsPresent){
//                    createResponseMessageWithIDRecord(schemas);
//
//
//                }
//                createResponseMessageRecord(dispatcherKey, dispatcherStreamId, schemas);
                for (Map.Entry<String, Schema> schema : schemas.entrySet()) {
                    String schemaKey = schema.getKey().trim();
                    //TODO: thushalya :- check this after uncomment hasConstraints
//                    if (!hasConstraints) {
//                        hasConstraints = GeneratorUtils.hasConstraints(schema.getValue());
//                    }
                    if (GeneratorUtils.isValidSchemaName(schemaKey)) {
                        List<Node> schemaDoc = new ArrayList<>();
                        typeDefinitionNodeListForSchema.add(getTypeDefinitionNode(
                                (AsyncApi25SchemaImpl) schema.getValue(), schemaKey, schemaDoc));
                    }
                }
            }
        }

        //Create imports for the http module, when record has http type inclusions.
        NodeList<ImportDeclarationNode> imports = generateImportNodes();
        typeDefinitionNodeList.addAll(typeDefinitionNodeListForSchema);
        // Create module member declaration
        NodeList<ModuleMemberDeclarationNode> moduleMembers = AbstractNodeFactory.createNodeList(
                typeDefinitionNodeList.toArray(new TypeDefinitionNode[typeDefinitionNodeList.size()]));

        Token eofToken = AbstractNodeFactory.createIdentifierToken("");
        ModulePartNode modulePartNode = NodeFactory.createModulePartNode(imports, moduleMembers, eofToken);

        TextDocument textDocument = TextDocuments.from("");
        SyntaxTree syntaxTree = SyntaxTree.from(textDocument);
        return syntaxTree.modifyWith(modulePartNode);
    }

//    private void createResponseMessageWithIDRecord(Map<String, Schema> schemas) {
//        //create ResponseMessage record
//        AsyncApi25SchemaImpl responseMessageWithId = new AsyncApi25SchemaImpl();
//        responseMessageWithId.setType(OBJECT);
//        AsyncApi25SchemaImpl stringEventSchema = new AsyncApi25SchemaImpl();
//        AsyncApi25SchemaImpl stringIdSchema = new AsyncApi25SchemaImpl();
//
//        stringEventSchema.setType(STRING);
//        stringIdSchema.setType(STRING);
//        List requiredFields = new ArrayList();
//        requiredFields.add(dispatcherKey);
//        requiredFields.add(dispatcherStreamId);
//
//        responseMessageWithId.setRequired(requiredFields);
//        responseMessageWithId.addProperty(dispatcherKey, stringEventSchema);
//        responseMessageWithId.addProperty(dispatcherStreamId, stringIdSchema);
//        schemas.put(RESPONSE_MESSAGE_WITH_ID_VAR_NAME, responseMessageWithId);
//    }

//    private void createResponseMessageRecord(Map<String, Schema> schemas) {
//        //create ResponseMessage record
//        AsyncApi25SchemaImpl responseMessage = new AsyncApi25SchemaImpl();
//        responseMessage.setType(OBJECT);
//        AsyncApi25SchemaImpl stringEventSchema = new AsyncApi25SchemaImpl();
//
//        stringEventSchema.setType(STRING);
//        List requiredFields = new ArrayList();
//        requiredFields.add(dispatcherKey);
//
//        responseMessage.setRequired(requiredFields);
//        responseMessage.addProperty(dispatcherKey, stringEventSchema);
//        schemas.put(RESPONSE_MESSAGE, responseMessage);
//    }


    private NodeList<ImportDeclarationNode> generateImportNodes() {
        List<ImportDeclarationNode> imports = new ArrayList<>();
        if (!typeDefinitionNodeList.isEmpty()) {
            importsForTypeDefinitions(imports);
        }
//        boolean nullable = GeneratorMetaData.getInstance().isNullable();
        if (hasConstraints) {
            //import for constraint
            ImportDeclarationNode importForConstraint = GeneratorUtils.getImportDeclarationNode(
                    GeneratorConstants.BALLERINA,
                    GeneratorConstants.CONSTRAINT);
            imports.add(importForConstraint);
        }
        if (imports.isEmpty()) {
            return createEmptyNodeList();
        }
        return createNodeList(imports);
    }

    private void importsForTypeDefinitions(List<ImportDeclarationNode> imports) {
        for (TypeDefinitionNode node : typeDefinitionNodeList) {
            if (!(node.typeDescriptor() instanceof RecordTypeDescriptorNode)) {
                continue;
            }
            if (node.typeName().text().equals(CONNECTION_CONFIG)) {
                ImportDeclarationNode importForWebsocket = GeneratorUtils.getImportDeclarationNode(
                        GeneratorConstants.BALLERINA,
                        WEBSOCKET);
                ImportDeclarationNode importForHttp = GeneratorUtils.getImportDeclarationNode(
                        GeneratorConstants.BALLERINA,
                        HTTP);
                imports.add(importForWebsocket);
                imports.add(importForHttp);
            }
//            RecordTypeDescriptorNode record = (RecordTypeDescriptorNode) node.typeDescriptor();
//            for (Node field : record.fields()) {
//                if (!(field instanceof TypeReferenceNode) ||
//                        !(((TypeReferenceNode) field).typeName() instanceof QualifiedNameReferenceNode)) {
//                    continue;
//                }
//                TypeReferenceNode recordField = (TypeReferenceNode) field;
//                QualifiedNameReferenceNode typeInclusion = (QualifiedNameReferenceNode) recordField.typeName();
//                boolean isHttpImportExist = imports.stream().anyMatch(importNode -> importNode.moduleName().stream()
//                        .anyMatch(moduleName -> moduleName.text().equals(WEBSOCKET)));
//
//                if (!isHttpImportExist && typeInclusion.modulePrefix().text().equals(WEBSOCKET)) {
//                    ImportDeclarationNode importForHttp = GeneratorUtils.getImportDeclarationNode(
//                            GeneratorConstants.BALLERINA,
//                            WEBSOCKET);
//                    imports.add(importForHttp);
//                    break;
//                }
//            }
        }
    }
}
