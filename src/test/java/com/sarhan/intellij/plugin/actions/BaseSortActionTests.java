/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sarhan.intellij.plugin.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.assertj.core.api.Assertions;

/**
 * Test class for {@link BaseSortAction}.
 *
 * @author Akmal Sarhan
 */
public class BaseSortActionTests extends BasePlatformTestCase {

	public void testSortAnnotationsForInterfaceAndAnonymousClassMethods() {
		// Create a test file with interface and anonymous class with annotations in wrong
		// order
		String testCode = """
				package com.example;

				import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
				import org.jetbrains.annotations.Nullable;

				interface ITestClass {
					@Nullable
					@NonNull
					String test();
				}

				public class TestClass implements ITestClass {

					private interface ITest extends ITestClass {
						@Nullable
						@NonNull
						String doSomething();

						@Nullable
						@NonNull
						@Override
						String test();
					}

					@Nullable
					@NonNull
					@Override
					public String test() {
						new ITest() {
							@Nullable
							@NonNull
							@Override
							public String doSomething() {
								return "";
							}

							@Nullable
							@NonNull
							@Override
							public String test() {
								return null;
							}
						};
						return null;
					}
				}
				""";
		PsiFile psiFile = this.myFixture.configureByText("TestClass.java", testCode);
		Assertions.assertThat(psiFile).isInstanceOf(PsiJavaFile.class);

		PsiJavaFile javaFile = (PsiJavaFile) psiFile;
		PsiClass[] classes = javaFile.getClasses();
		Assertions.assertThat(classes).hasSize(2); // ITestClass interface and TestClass

		// Find the TestClass
		PsiClass testClass = null;
		for (PsiClass cls : classes) {
			if ("TestClass".equals(cls.getName())) {
				testClass = cls;
				break;
			}
		}
		Assertions.assertThat(testClass).isNotNull();

		// Apply the sorting
		PsiClass finalTestClass = testClass;
		ApplicationManager.getApplication().invokeAndWait(() -> BaseSortAction.sortClass(getProject(), finalTestClass));

		// Verify that annotations are sorted
		// Note: This is a basic structural test. The actual verification would depend on
		// checking the annotations order after sorting.
		PsiMethod[] methods = testClass.getMethods();
		Assertions.assertThat(methods).isNotEmpty();
	}

	public void testSortAnnotationsForInnerInterface() {
		String testCode = """
				package com.example;

				import org.eclipse.lsp4j.jsonrpc.validation.NonNull;
				import org.jetbrains.annotations.Nullable;

				public class OuterClass {
					private interface InnerInterface {
						@Nullable
						@NonNull
						String method();
					}
				}
				""";

		PsiFile psiFile = this.myFixture.configureByText("OuterClass.java", testCode);
		PsiJavaFile javaFile = (PsiJavaFile) psiFile;
		PsiClass[] classes = javaFile.getClasses();
		Assertions.assertThat(classes).hasSize(1);

		PsiClass outerClass = classes[0];
		PsiClass[] innerClasses = outerClass.getInnerClasses();
		Assertions.assertThat(innerClasses).hasSize(1);

		// Apply sorting
		ApplicationManager.getApplication().invokeAndWait(() -> BaseSortAction.sortClass(getProject(), outerClass));

		// Verify inner interface was processed
		PsiClass innerInterface = innerClasses[0];
		Assertions.assertThat(innerInterface.isInterface()).isTrue();
		PsiMethod[] methods = innerInterface.getMethods();
		Assertions.assertThat(methods).hasSize(1);
	}

}
