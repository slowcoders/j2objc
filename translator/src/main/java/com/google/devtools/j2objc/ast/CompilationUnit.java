/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.j2objc.ast;

import java.io.InvalidClassException;
import java.util.HashMap;
import java.util.List;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.devtools.j2objc.J2ObjC;
import com.google.devtools.j2objc.javac.ImportManager;
import com.google.devtools.j2objc.util.TranslationEnvironment;
import com.google.devtools.j2objc.util.UnicodeUtils;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;

/**
 * Tree node for a Java compilation unit.
 */
public class CompilationUnit extends TreeNode {

  private final TranslationEnvironment env;
  private final String sourceFilePath;
  private final String mainTypeName;
  private final String source;
  private final int[] newlines;
  
  private boolean hasIncompleteProtocol = false;
  private boolean hasIncompleteImplementation = false;
  private boolean hasNullabilityAnnotations = false;
  private final ChildLink<PackageDeclaration> packageDeclaration =
      ChildLink.create(PackageDeclaration.class, this);
  private final ChildList<Comment> comments = ChildList.create(Comment.class, this);
  private final ChildList<NativeDeclaration> nativeBlocks =
      ChildList.create(NativeDeclaration.class, this);
  private final ChildList<AbstractTypeDeclaration> types =
      ChildList.create(AbstractTypeDeclaration.class, this);
  private HashMap<String, String> unreachableImports;

  public CompilationUnit(TranslationEnvironment env, String mainTypeName) {
    this(env, "", mainTypeName, "");
  }

  public CompilationUnit(
      TranslationEnvironment env, String sourceFilePath, String mainTypeName, String source) {
    super();
    this.env = env;
    this.sourceFilePath = Preconditions.checkNotNull(sourceFilePath);
    this.mainTypeName = Preconditions.checkNotNull(mainTypeName);
    this.source = Preconditions.checkNotNull(source);
    this.newlines = findNewlines(source);
  }

  public CompilationUnit(CompilationUnit other) {
    super(other);
    this.env = other.env;
    sourceFilePath = other.getSourceFilePath();
    mainTypeName = other.getMainTypeName();
    source = other.getSource();
    newlines = new int[other.newlines.length];
    System.arraycopy(other.newlines, 0, newlines, 0, newlines.length);
    packageDeclaration.copyFrom(other.getPackage());
    comments.copyFrom(other.getCommentList());
    nativeBlocks.copyFrom(other.getNativeBlocks());
    types.copyFrom(other.getTypes());
  }

  @Override
  public Kind getKind() {
    return Kind.COMPILATION_UNIT;
  }

  public TranslationEnvironment getEnv() {
    return env;
  }

  public String getSourceFilePath() {
    return sourceFilePath;
  }

  public String getMainTypeName() {
    return mainTypeName;
  }

  public String getSource() {
    return source;
  }

  public boolean hasIncompleteProtocol() {
    return hasIncompleteProtocol;
  }

  public void setHasIncompleteProtocol() {
    hasIncompleteProtocol = true;
  }

  public boolean hasIncompleteImplementation() {
    return hasIncompleteImplementation;
  }

  public void setHasIncompleteImplementation() {
    hasIncompleteImplementation = true;
  }

  public boolean hasNullabilityAnnotations() {
    return hasNullabilityAnnotations;
  }

  public void setHasNullabilityAnnotations() {
    hasNullabilityAnnotations = true;
  }

  public PackageDeclaration getPackage() {
    return packageDeclaration.get();
  }

  public void setPackage(PackageDeclaration newPackageDeclaration) {
    packageDeclaration.set(newPackageDeclaration);
  }

  public List<Comment> getCommentList() {
    return comments;
  }

  public List<NativeDeclaration> getNativeBlocks() {
    return nativeBlocks;
  }

  public List<AbstractTypeDeclaration> getTypes() {
    return types;
  }

  public int getLineNumber(int position) {
    if (position < 0 || position >= source.length()) {
      return -1;
    }
    return getLineNumber(position, 0, newlines.length - 1);
  }

  private int getLineNumber(int position, int start, int end) {
    if (start == end) {
      return start + 1;
    }
    int middle = (start + end + 1) / 2;
    if (newlines[middle] > position) {
      return getLineNumber(position, start, middle - 1);
    } else {
      return getLineNumber(position, middle, end);
    }
  }

  private static int[] findNewlines(String source) {
    if (source.isEmpty()) {
      return new int[0];
    }
    List<Integer> newlinesList = Lists.newArrayList();
    newlinesList.add(0);
    int len = source.length();
    for (int i = 0; i < len; i++) {
      if (source.charAt(i) == '\n') {
        newlinesList.add(i + 1);
      }
    }
    int size = newlinesList.size();
    int[] newlines = new int[size];
    for (int i = 0; i < size; i++) {
      newlines[i] = newlinesList.get(i);
    }
    return newlines;
  }

  @Override
  protected void acceptInner(TreeVisitor visitor) {
    if (visitor.visit(this)) {
      packageDeclaration.accept(visitor);
      comments.accept(visitor);
      nativeBlocks.accept(visitor);
      types.accept(visitor);
    }
    visitor.endVisit(this);
  }

  @Override
  public CompilationUnit copy() {
    return new CompilationUnit(this);
  }

  @Override
  public void validateInner() {
    super.validateInner();
    Preconditions.checkNotNull(sourceFilePath);
    Preconditions.checkNotNull(mainTypeName);
    Preconditions.checkNotNull(source);
    Preconditions.checkNotNull(packageDeclaration);
  }

  public CompilationUnit addComment(Comment comment) {
    comments.add(comment);
    return this;
  }

  public CompilationUnit addNativeBlock(NativeDeclaration decl) {
    nativeBlocks.add(decl);
    return this;
  }

  public CompilationUnit addType(AbstractTypeDeclaration type) {
    types.add(type);
    return this;
  }

  public CompilationUnit addType(int index, AbstractTypeDeclaration type) {
    types.add(index, type);
    return this;
  }

  public HashMap<String, String> getUnreachableImportedClasses() {
	  return this.unreachableImports;
  }
  
  public HashMap<String, String> resolveUnreachableImportedClasses(CompilationUnitTree unit) {
    HashMap<String, String> map = new HashMap<>();
    for (ImportTree tree : unit.getImports()) {
      String fullname = tree.getQualifiedIdentifier().toString();
      if (!ImportManager.canImportClass(fullname)) {
        String simpleName = fullname.substring(fullname.lastIndexOf('.') + 1);
        map.put(simpleName, fullname);
      }
    }
	  this.unreachableImports = map;
	  return map;
  }

  static HashMap<String, HashMap<String, Integer>> stringPools = new HashMap<>();
  
  public String getStringConstant(String literalValue) {
	  String sp = this.getSourceFilePath();
	  HashMap<String, Integer> stringPool = stringPools.get(sp);
	  if (stringPool == null) {
		  stringPool = new HashMap<>();
		  stringPools.put(this.getSourceFilePath(), stringPool);
	  }
	  Integer idx = stringPool.get(literalValue);
	  if (idx == null) {
		  idx = (stringPool.size() + 1);
		  stringPool.put(literalValue, idx);
	  }
	  
	  if (!UnicodeUtils.hasValidCppCharacters(literalValue)) {
		  return "JreString(" + idx + ", __)";
	  }
	  else {
		  return "JreString(" + idx + ", \"" + UnicodeUtils.escapeStringLiteral(literalValue) + "\")";
	  }
  }

  public static HashMap<String, Integer> getStringPool(String sourceFileName) {
    HashMap<String, Integer> stringPool = stringPools.get(sourceFileName);
    return stringPool;	
  }

  public void resolveTestCase() {
    for (AbstractTypeDeclaration _t : this.getTypes()) {
      TypeElement type = _t.getTypeElement();
      try {
        boolean isTestCase = false;
        for (BodyDeclaration body : _t.getBodyDeclarations()) {
          if (body instanceof MethodDeclaration) {
            isTestCase |= ((MethodDeclaration)body).checkTestMethod();
          }
        }
        if (isTestCase) {
          testcaseClasses.put(type.asType(), type.asType()); 
        }
      } catch (InvalidClassException e) {
        System.err.println("Testcase conversion error: " + sourceFilePath + 
            "\nTest method name must start with 'test'." +
            "\nThe name of method annotated by @Before must be 'setUp'" +
            "\nThe name of method annotated by @After must be 'tearDown'");
      }
    }
  }

  private static HashMap<TypeMirror, TypeMirror> testcaseClasses = new HashMap<>(); 

  public static boolean isTestClass(TypeMirror type) {
    return testcaseClasses.containsKey(type);
  }
  
}
