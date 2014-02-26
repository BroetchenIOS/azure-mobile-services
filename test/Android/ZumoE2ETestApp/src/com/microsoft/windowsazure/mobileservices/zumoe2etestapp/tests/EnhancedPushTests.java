/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
Apache 2.0 License
 
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
 
     http://www.apache.org/licenses/LICENSE-2.0
 
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 
See the Apache Version 2.0 License for specific language governing permissions and limitations under the License.
 */
package com.microsoft.windowsazure.mobileservices.zumoe2etestapp.tests;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.MobileServiceException;
import com.microsoft.windowsazure.mobileservices.MobileServicePush;
import com.microsoft.windowsazure.mobileservices.Registration;
import com.microsoft.windowsazure.mobileservices.RegistrationCallback;
import com.microsoft.windowsazure.mobileservices.TemplateRegistration;
import com.microsoft.windowsazure.mobileservices.TemplateRegistrationCallback;
import com.microsoft.windowsazure.mobileservices.UnregisterAllCallback;
import com.microsoft.windowsazure.mobileservices.UnregisterCallback;
import com.microsoft.windowsazure.mobileservices.UnregisterTemplateCallback;
import com.microsoft.windowsazure.mobileservices.zumoe2etestapp.MainActivity;
import com.microsoft.windowsazure.mobileservices.zumoe2etestapp.framework.TestCase;
import com.microsoft.windowsazure.mobileservices.zumoe2etestapp.framework.TestExecutionCallback;
import com.microsoft.windowsazure.mobileservices.zumoe2etestapp.framework.TestGroup;
import com.microsoft.windowsazure.mobileservices.zumoe2etestapp.framework.TestResult;
import com.microsoft.windowsazure.mobileservices.zumoe2etestapp.framework.TestStatus;

public class EnhancedPushTests extends TestGroup {

	/*
	 * Pointer to the main activity used to register with GCM
	 */
	public static MainActivity mainActivity;

	protected static final String ROUND_TRIP_TABLE_NAME = "droidRoundTripTable";
	protected static final String PARAM_TEST_TABLE_NAME = "ParamsTestTable";

	private static final String DEFAULT_REGISTRATION_NAME = "$Default";
	private static final String REGISTRATION_NAME_STORAGE_KEY = "__NH_REG_NAME_";

	private void register(TestCase test, MobileServicePush hub, String gcmId, String[] tags, final RegistrationCallback callback) {
		test.log("Register Native with GCMID = " + gcmId);
		if (tags != null && tags.length > 0) {
			for (String tag : tags) {
				test.log("Using tag: " + tag);
			}
		}

		hub.register(gcmId, tags, callback);
	}

	private void unregister(TestCase test, MobileServicePush hub, UnregisterCallback callback) {
		test.log("Unregister Native");
		hub.unregister(callback);
	}

	private void registerTemplate(TestCase test, MobileServicePush hub, String gcmId, String templateName, String[] tags, TemplateRegistrationCallback callback) {
		String template = "{\"time_to_live\": 108, \"delay_while_idle\": true, \"data\": { \"message\": \"$(msg)\" } }";
		registerTemplate(test, hub, gcmId, templateName, template, tags, callback);
	}

	private void registerTemplate(TestCase test, MobileServicePush hub, String gcmId, String templateName, String template, String[] tags,
			TemplateRegistrationCallback callback) {

		test.log("Register with GCMID = " + gcmId);
		test.log("Register with templateName = " + templateName);

		if (tags != null && tags.length > 0) {
			for (String tag : tags) {
				test.log("Using tag: " + tag);
			}
		}

		hub.registerTemplate(gcmId, templateName, template, tags, callback);
	}

	private void unregisterTemplate(TestCase test, MobileServicePush hub, String templateName, final UnregisterTemplateCallback callback) {
		test.log("UnregisterTemplate with templateName = " + templateName);

		hub.unregisterTemplate(templateName, callback);
	}

	private void unregisterAll(TestCase test, MobileServicePush hub, String gcmId, final UnregisterAllCallback callback) {
		test.log("Unregister Native");
		hub.unregisterAll(gcmId, callback);
	}

	public static void clearNotificationHubStorageData() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.getInstance());
		Editor editor = sharedPreferences.edit();
		Set<String> keys = sharedPreferences.getAll().keySet();

		for (String key : keys) {
			if (key.startsWith("__NH_")) {
				editor.remove(key);
			}
		}

		editor.commit();
	}

	public static void clearRegistrationsStorageData() {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.getInstance());
		Editor editor = sharedPreferences.edit();
		Set<String> keys = sharedPreferences.getAll().keySet();

		for (String key : keys) {
			if (key.startsWith("REGISTRATION_NAME_STORAGE_KEY")) {
				editor.remove(key);
			}
		}

		editor.commit();
	}
	
	private void addUnexistingNativeRegistration(String registrationId) {
		addUnexistingTemplateRegistration(DEFAULT_REGISTRATION_NAME, registrationId);
	}

	private void addUnexistingTemplateRegistration(String templateName, String registrationId) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.getInstance());

		Editor editor = sharedPreferences.edit();
		editor.putString(REGISTRATION_NAME_STORAGE_KEY + templateName, registrationId);
		editor.commit();
	}

	private int getRegistrationCountInLocalStorage() {
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.getInstance());
		int regCount = 0;

		for (String key : preferences.getAll().keySet()) {
			if (key.startsWith("__NH_REG_NAME_")) {
				regCount++;
			}
		}

		return regCount;
	}

	private boolean matchTags(final String[] tags, List<String> regTags) {
		if (tags == null || regTags == null) {
			return (tags == null && regTags == null) || (tags == null && regTags.size() == 0) || (regTags == null && tags.length == 0);
		} else if (regTags.size() != tags.length) {
			return false;
		} else {
			for (String tag : tags) {
				if (!regTags.contains(tag)) {
					return false;
				}
			}
		}

		return true;
	}

	// Register Native Tests

	private TestCase createRegisterNativeTestCase(String name, final String[] tags) {

		TestCase register = new TestCase() {

			@Override
			protected void executeTest(MobileServiceClient client, final TestExecutionCallback callback) {
				final TestCase that = this;

				try {
					final MobileServicePush MobileServicePush = client.getPush();
					final TestResult result = new TestResult();
					result.setStatus(TestStatus.Passed);
					result.setTestCase(this);

					register(this, MobileServicePush, UUID.randomUUID().toString(), tags, new RegistrationCallback() {

						@Override
						public void onRegister(Registration reg, Exception exception) {

							if (exception != null) {
								callback.onTestComplete(that, createResultFromException(exception));
								return;

							}
							if (!matchTags(tags, reg.getTags())) {
								result.setStatus(TestStatus.Failed);
							}

							unregister(that, MobileServicePush, new UnregisterCallback() {
								@Override
								public void onUnregister(Exception exception) {

									if (exception != null) {
										callback.onTestComplete(that, createResultFromException(exception));
										return;

									}

									callback.onTestComplete(that, result);
									return;
								}

							});
						}

					});

				} catch (Exception e) {
					callback.onTestComplete(this, createResultFromException(e));
					return;
				}
			}
		};

		register.setName(name);

		return register;
	}

	private TestCase createRegisterNativeTwiceTestCase(String name, final String[] firstTags, final String[] lastTags) {
		TestCase register = new TestCase() {

			@Override
			protected void executeTest(MobileServiceClient client, final TestExecutionCallback callback) {
				final TestCase that = this;

				try {
					final MobileServicePush MobileServicePush = client.getPush();
					final TestResult result = new TestResult();
					result.setStatus(TestStatus.Passed);
					result.setTestCase(this);

					String gcmId = UUID.randomUUID().toString();

					register(this, MobileServicePush, gcmId, firstTags, new RegistrationCallback() {

						@Override
						public void onRegister(Registration reg, Exception exception) {

							if (exception != null) {
								callback.onTestComplete(that, createResultFromException(exception));
								return;
							}

							String gcmIdTwo = UUID.randomUUID().toString();

							register(that, MobileServicePush, gcmIdTwo, lastTags, new RegistrationCallback() {

								@Override
								public void onRegister(Registration reg, Exception exception) {

									if (exception != null) {
										callback.onTestComplete(that, createResultFromException(exception));
										return;
									}

									if (!matchTags(lastTags, reg.getTags())) {
										result.setStatus(TestStatus.Failed);
									}

									if (getRegistrationCountInLocalStorage() != 1) {
										result.setStatus(TestStatus.Failed);
									}

									unregister(that, MobileServicePush, new UnregisterCallback() {

										@Override
										public void onUnregister(Exception exception) {
											if (exception != null) {
												callback.onTestComplete(that, createResultFromException(exception));
												return;
											}

											callback.onTestComplete(that, result);
											return;
										}

									});

								}
							});
						}
					});

				} catch (Exception e) {
					callback.onTestComplete(this, createResultFromException(e));
					return;
				}
			}
		};
		register.setName(name);

		return register;
	}

	private TestCase createReRegisterNativeTestCase(String name) {
		TestCase register = new TestCase() {

			@Override
			protected void executeTest(MobileServiceClient client, final TestExecutionCallback callback) {
				final TestCase that = this;

				try {
					final MobileServicePush MobileServicePush = client.getPush();
					final TestResult result = new TestResult();
					result.setStatus(TestStatus.Passed);
					result.setTestCase(this);

					final String gcmId = UUID.randomUUID().toString();

					register(this, MobileServicePush, gcmId, (String[]) null, new RegistrationCallback() {

						@Override
						public void onRegister(final Registration reg1, Exception exception) {

							if (exception != null) {
								callback.onTestComplete(that, createResultFromException(exception));
								return;
							}

							unregister(that, MobileServicePush, new UnregisterCallback() {

								@Override
								public void onUnregister(Exception exception) {
									if (exception != null) {
										callback.onTestComplete(that, createResultFromException(exception));
										return;
									}

									register(that, MobileServicePush, gcmId, (String[]) null, new RegistrationCallback() {

										@Override
										public void onRegister(Registration reg2, Exception exception) {

											if (exception != null) {
												callback.onTestComplete(that, createResultFromException(exception));
												return;
											}

											if (reg2.getRegistrationId().equals(reg1.getRegistrationId())) {
												result.setStatus(TestStatus.Failed);
											}

											unregister(that, MobileServicePush, new UnregisterCallback() {

												@Override
												public void onUnregister(Exception exception) {
													if (exception != null) {
														callback.onTestComplete(that, createResultFromException(exception));
														return;
													}

													callback.onTestComplete(that, result);
												}
											});
										}
									});
								}
							});
						}
					});
				} catch (Exception e) {
					callback.onTestComplete(this, createResultFromException(e));
					return;
				}
			}
		};
		register.setName(name);

		return register;
	}

	private TestCase createRegisterNativeEmptyGcmRegistrationIdTestCase(String name) {
		TestCase register = new TestCase() {

			@Override
			protected void executeTest(MobileServiceClient client, final TestExecutionCallback callback) {
				final TestCase that = this;

				try {
					final MobileServicePush MobileServicePush = client.getPush();
					final TestResult result = new TestResult();
					result.setStatus(TestStatus.Passed);
					result.setTestCase(this);

					register(this, MobileServicePush, "", (String[]) null, new RegistrationCallback() {

						@Override
						public void onRegister(Registration reg2, Exception exception) {

							if (exception != null) {
								callback.onTestComplete(that, createResultFromException(exception));
								return;
							}

							callback.onTestComplete(that, result);
						}
					});

				} catch (Exception e) {
					callback.onTestComplete(this, createResultFromException(e));
				}
			}
		};
		register.setName(name);
		register.setExpectedExceptionClass(IllegalArgumentException.class);

		return register;
	}

	// Register Template Tests

	private TestCase createRegisterTemplateTestCase(String name, final String templateName, final String[] tags) {
		TestCase register = new TestCase() {

			@Override
			protected void executeTest(MobileServiceClient client, final TestExecutionCallback callback) {
				final TestCase that = this;

				try {
					final MobileServicePush MobileServicePush = client.getPush();
					final TestResult result = new TestResult();
					result.setStatus(TestStatus.Passed);
					result.setTestCase(this);

					final String template = "{\"time_to_live\": 108, \"delay_while_idle\": true, \"data\": { \"message\": \"$(msg)\" } }";

					registerTemplate(this, MobileServicePush, UUID.randomUUID().toString(), templateName, template, tags, new TemplateRegistrationCallback() {

						@Override
						public void onRegister(TemplateRegistration reg, Exception exception) {
							if (exception != null) {
								callback.onTestComplete(that, createResultFromException(exception));
								return;
							}

							if (!(reg.getBodyTemplate() != null && reg.getBodyTemplate().equals(template))) {
								result.setStatus(TestStatus.Failed);
							}

							if (!matchTags(tags, reg.getTags())) {
								result.setStatus(TestStatus.Failed);
							}

							unregisterTemplate(that, MobileServicePush, templateName, new UnregisterTemplateCallback() {

								@Override
								public void onUnregister(Exception exception) {
									if (exception != null) {
										callback.onTestComplete(that, createResultFromException(exception));
										return;
									}

									callback.onTestComplete(that, result);
									return;
								}

							});

						}

					});

				} catch (Exception e) {
					callback.onTestComplete(this, createResultFromException(e));
					return;
				}
			}
		};
		register.setName(name);

		return register;
	}

	private TestCase createRegisterTemplateTwiceTestCase(String name, final String templateName, final String[] firstTags, final String[] lastTags) {
		TestCase register = new TestCase() {

			@Override
			protected void executeTest(MobileServiceClient client, final TestExecutionCallback callback) {
				final TestCase that = this;

				try {
					final MobileServicePush MobileServicePush = client.getPush();
					final TestResult result = new TestResult();
					result.setStatus(TestStatus.Passed);
					result.setTestCase(this);

					String gcmId = UUID.randomUUID().toString();

					registerTemplate(this, MobileServicePush, gcmId, templateName, firstTags, new TemplateRegistrationCallback() {

						@Override
						public void onRegister(TemplateRegistration reg, Exception exception) {
							if (exception != null) {
								callback.onTestComplete(that, createResultFromException(exception));
								return;
							}

							String gcmIdTwo = UUID.randomUUID().toString();

							final String newTemplate = "{\"time_to_live\": 21, \"delay_while_idle\": false, \"data\": { \"message\": \"$(msg)\" } }";

							registerTemplate(that, MobileServicePush, gcmIdTwo, templateName, newTemplate, lastTags, new TemplateRegistrationCallback() {

								@Override
								public void onRegister(TemplateRegistration reg, Exception exception) {
									if (exception != null) {
										callback.onTestComplete(that, createResultFromException(exception));
										return;
									}

									if (!(reg.getBodyTemplate() != null && reg.getBodyTemplate().equals(newTemplate))) {
										result.setStatus(TestStatus.Failed);
									}

									if (!matchTags(lastTags, reg.getTags())) {
										result.setStatus(TestStatus.Failed);
									}

									if (getRegistrationCountInLocalStorage() != 1) {
										result.setStatus(TestStatus.Failed);
									}

									unregisterTemplate(that, MobileServicePush, templateName, new UnregisterTemplateCallback() {

										@Override
										public void onUnregister(Exception exception) {
											if (exception != null) {
												callback.onTestComplete(that, createResultFromException(exception));
												return;
											}

											callback.onTestComplete(that, result);
											return;
										}

									});
								}
							});

						}
					});

				} catch (Exception e) {
					callback.onTestComplete(this, createResultFromException(e));
					return;
				}
			}
		};
		register.setName(name);

		return register;
	}

	private TestCase createRegisterTwoTemplatesTestCase(String name, final String templateName1, final String templateName2) {
		TestCase register = new TestCase() {

			@Override
			protected void executeTest(MobileServiceClient client, final TestExecutionCallback callback) {

				final TestCase that = this;

				try {
					final MobileServicePush MobileServicePush = client.getPush();
					final TestResult result = new TestResult();
					result.setStatus(TestStatus.Passed);
					result.setTestCase(this);

					final String gcmId = UUID.randomUUID().toString();

					registerTemplate(this, MobileServicePush, gcmId, templateName1, (String[]) null, new TemplateRegistrationCallback() {

						@Override
						public void onRegister(TemplateRegistration reg, Exception exception) {
							if (exception != null) {
								callback.onTestComplete(that, createResultFromException(exception));
								return;
							}

							registerTemplate(that, MobileServicePush, gcmId, templateName2, (String[]) null, new TemplateRegistrationCallback() {

								@Override
								public void onRegister(TemplateRegistration reg, Exception exception) {
									if (exception != null) {
										callback.onTestComplete(that, createResultFromException(exception));
										return;
									}

									if (getRegistrationCountInLocalStorage() != 2) {
										result.setStatus(TestStatus.Failed);
									}

									unregisterTemplate(that, MobileServicePush, templateName1, new UnregisterTemplateCallback() {

										@Override
										public void onUnregister(Exception exception) {
											if (exception != null) {
												callback.onTestComplete(that, createResultFromException(exception));
												return;
											}

											unregisterTemplate(that, MobileServicePush, templateName2, new UnregisterTemplateCallback() {

												@Override
												public void onUnregister(Exception exception) {
													if (exception != null) {
														callback.onTestComplete(that, createResultFromException(exception));
														return;
													}

													callback.onTestComplete(that, result);
													return;
												}

											});
										}

									});
								}
							});
						}
					});

					callback.onTestComplete(this, result);
					return;

				} catch (Exception e) {
					callback.onTestComplete(this, createResultFromException(e));
					return;
				}
			}
		};
		register.setName(name);

		return register;
	}

	private TestCase createReRegisterTemplateTestCase(String name, final String templateName) {
		TestCase register = new TestCase() {

			@Override
			protected void executeTest(MobileServiceClient client, final TestExecutionCallback callback) {
				final TestCase that = this;

				try {
					final MobileServicePush MobileServicePush = client.getPush();
					final TestResult result = new TestResult();
					result.setStatus(TestStatus.Passed);
					result.setTestCase(this);

					final String gcmId = UUID.randomUUID().toString();

					registerTemplate(this, MobileServicePush, gcmId, templateName, (String[]) null, new TemplateRegistrationCallback() {

						@Override
						public void onRegister(final TemplateRegistration reg1, Exception exception) {
							if (exception != null) {
								callback.onTestComplete(that, createResultFromException(exception));
								return;
							}

							unregisterTemplate(that, MobileServicePush, templateName, new UnregisterTemplateCallback() {

								@Override
								public void onUnregister(Exception exception) {
									if (exception != null) {
										callback.onTestComplete(that, createResultFromException(exception));
										return;
									}

									registerTemplate(that, MobileServicePush, gcmId, templateName, (String[]) null, new TemplateRegistrationCallback() {

										@Override
										public void onRegister(TemplateRegistration reg2, Exception exception) {
											if (exception != null) {
												callback.onTestComplete(that, createResultFromException(exception));
												return;
											}

											if (reg2.getRegistrationId().equals(reg1.getRegistrationId())) {
												result.setStatus(TestStatus.Failed);
											}

											unregisterTemplate(that, MobileServicePush, templateName, new UnregisterTemplateCallback() {

												@Override
												public void onUnregister(Exception exception) {
													if (exception != null) {
														callback.onTestComplete(that, createResultFromException(exception));
														return;
													}

													callback.onTestComplete(that, result);

													return;
												}
											});
										}
									});
								}
							});

						}
					});

				} catch (Exception e) {
					callback.onTestComplete(this, createResultFromException(e));
					return;
				}
			}
		};
		register.setName(name);

		return register;
	}

	private TestCase createRegisterTemplateEmptyGCMRegistrationIdTestCase(String name, final String templateName) {
		TestCase register = new TestCase() {

			@Override
			protected void executeTest(MobileServiceClient client, final TestExecutionCallback callback) {
				final TestCase that = this;

				try {
					final MobileServicePush MobileServicePush = client.getPush();
					final TestResult result = new TestResult();
					result.setStatus(TestStatus.Passed);
					result.setTestCase(this);

					registerTemplate(this, MobileServicePush, "", templateName, (String[]) null, new TemplateRegistrationCallback() {

						@Override
						public void onRegister(TemplateRegistration registration, Exception exception) {
							if (exception != null) {
								callback.onTestComplete(that, createResultFromException(exception));
								return;
							}
							callback.onTestComplete(that, result);
							return;
						}
					});

				} catch (Exception e) {
					callback.onTestComplete(this, createResultFromException(e));
					return;
				}
			}
		};
		register.setName(name);
		register.setExpectedExceptionClass(IllegalArgumentException.class);

		return register;
	}

	private TestCase createRegisterTemplateEmptyTemplateNameTestCase(String name) {
		TestCase register = new TestCase() {

			@Override
			protected void executeTest(MobileServiceClient client, final TestExecutionCallback callback) {
				final TestCase that = this;

				try {
					final MobileServicePush MobileServicePush = client.getPush();
					final TestResult result = new TestResult();
					result.setStatus(TestStatus.Passed);
					result.setTestCase(this);

					registerTemplate(this, MobileServicePush, UUID.randomUUID().toString(), "", (String[]) null, new TemplateRegistrationCallback() {

						@Override
						public void onRegister(TemplateRegistration registration, Exception exception) {
							if (exception != null) {
								callback.onTestComplete(that, createResultFromException(exception));
								return;
							}
							callback.onTestComplete(that, result);
							return;
						}
					});

				} catch (Exception e) {
					callback.onTestComplete(this, createResultFromException(e));
					return;
				}
			}
		};
		register.setName(name);
		register.setExpectedExceptionClass(IllegalArgumentException.class);

		return register;
	}

	private TestCase createRegisterTemplateEmptyTemplateTestCase(String name, final String templateName) {
		TestCase register = new TestCase() {

			@Override
			protected void executeTest(MobileServiceClient client, final TestExecutionCallback callback) {
				final TestCase that = this;

				try {
					final MobileServicePush MobileServicePush = client.getPush();
					final TestResult result = new TestResult();
					result.setStatus(TestStatus.Passed);
					result.setTestCase(this);

					registerTemplate(this, MobileServicePush, UUID.randomUUID().toString(), templateName, "", (String[]) null,
							new TemplateRegistrationCallback() {

								@Override
								public void onRegister(TemplateRegistration registration, Exception exception) {
									if (exception != null) {
										callback.onTestComplete(that, createResultFromException(exception));
										return;
									}
									callback.onTestComplete(that, result);
									return;
								}
							});

				} catch (Exception e) {
					callback.onTestComplete(this, createResultFromException(e));
				}
			}
		};
		register.setName(name);
		register.setExpectedExceptionClass(IllegalArgumentException.class);

		return register;
	}

	private TestCase createReRegisterTemplateEmptyTemplateTestCase(String name, final String templateName) {
		TestCase register = new TestCase() {

			@Override
			protected void executeTest(MobileServiceClient client, final TestExecutionCallback callback) {
				final TestCase that = this;

				try {
					final MobileServicePush MobileServicePush = client.getPush();
					final TestResult result = new TestResult();
					result.setStatus(TestStatus.Passed);
					result.setTestCase(this);

					final String gcmId = UUID.randomUUID().toString();

					registerTemplate(this, MobileServicePush, gcmId, templateName, (String[]) null, new TemplateRegistrationCallback() {

						@Override
						public void onRegister(TemplateRegistration registration, Exception exception) {
							if (exception != null) {
								callback.onTestComplete(that, createResultFromException(exception));
								return;
							}

							registerTemplate(that, MobileServicePush, gcmId, templateName, "", (String[]) null, new TemplateRegistrationCallback() {

								@Override
								public void onRegister(TemplateRegistration registration, Exception exception) {
									if (exception != null) {
										callback.onTestComplete(that, createResultFromException(exception));
										return;
									}

									callback.onTestComplete(that, result);
									return;
								}
							});
						}
					});
				} catch (Exception e) {
					callback.onTestComplete(this, createResultFromException(e));
					return;
				}
			}
		};
		register.setName(name);
		register.setExpectedExceptionClass(IllegalArgumentException.class);

		return register;
	}

	// Unregister Native Tests
	private TestCase createUnregisterNativeNonExistingTestCase(String name) {
		TestCase register = new TestCase() {

			@Override
			protected void executeTest(MobileServiceClient client, final TestExecutionCallback callback) {

				final TestCase that = this;

				try {
					final MobileServicePush MobileServicePush = client.getPush();
					final TestResult result = new TestResult();
					result.setStatus(TestStatus.Passed);
					result.setTestCase(this);

					unregister(this, MobileServicePush, new UnregisterCallback() {

						@Override
						public void onUnregister(Exception exception) {
							if (exception != null) {
								callback.onTestComplete(that, createResultFromException(exception));
								return;
							}
							callback.onTestComplete(that, result);
							return;
						}

					});

				} catch (Exception e) {
					callback.onTestComplete(this, createResultFromException(e));
					return;
				}
			}
		};
		register.setName(name);

		return register;
	}

	private TestCase createUnregisterNativeUnexistingRegistrationTestCase(String name) {
		TestCase register = new TestCase() {

			@Override
			protected void executeTest(MobileServiceClient client, final TestExecutionCallback callback) {

				final TestCase that = this;

				try {
					final MobileServicePush MobileServicePush = client.getPush();
					final TestResult result = new TestResult();
					result.setStatus(TestStatus.Passed);
					result.setTestCase(this);

					String gcmId = UUID.randomUUID().toString();

					register(this, MobileServicePush, gcmId, (String[]) null, new RegistrationCallback() {

						@Override
						public void onRegister(Registration registration, Exception exception) {
							if (exception != null) {
								callback.onTestComplete(that, createResultFromException(exception));
								return;
							}

							final String registrationId = registration.getRegistrationId();

							unregister(that, MobileServicePush, new UnregisterCallback() {

								@Override
								public void onUnregister(Exception exception) {
									if (exception != null) {
										callback.onTestComplete(that, createResultFromException(exception));
										return;
									}

									addUnexistingNativeRegistration(registrationId);

									unregister(that, MobileServicePush, new UnregisterCallback() {

										@Override
										public void onUnregister(Exception exception) {
											if (exception != null) {
												callback.onTestComplete(that, createResultFromException(exception));
												return;
											}
											callback.onTestComplete(that, result);
											return;
										}

									});

								}
							});
						}
					});

				} catch (Exception e) {
					callback.onTestComplete(this, createResultFromException(e));
					return;
				}
			}
		};
		register.setName(name);
		register.setExpectedExceptionClass(MobileServiceException.class);

		return register;
	}

	// Unregister Template Tests

	private TestCase createUnregisterTemplateNonExistingTestCase(String name, final String templateName) {
		TestCase register = new TestCase() {

			@Override
			protected void executeTest(MobileServiceClient client, final TestExecutionCallback callback) {
				final TestCase that = this;
				try {
					final MobileServicePush MobileServicePush = client.getPush();
					final TestResult result = new TestResult();
					result.setStatus(TestStatus.Passed);
					result.setTestCase(this);

					unregisterTemplate(this, MobileServicePush, templateName, new UnregisterTemplateCallback() {

						@Override
						public void onUnregister(Exception exception) {
							if (exception != null) {
								callback.onTestComplete(that, createResultFromException(exception));
								return;
							}

							callback.onTestComplete(that, result);
							return;
						}
					});

				} catch (Exception e) {
					callback.onTestComplete(this, createResultFromException(e));
					return;
				}
			}
		};
		register.setName(name);

		return register;
	}

	private TestCase createUnregisterTemplateUnexistingRegistrationTestCase(String name, final String templateName) {
		TestCase register = new TestCase() {

			@Override
			protected void executeTest(MobileServiceClient client, final TestExecutionCallback callback) {
				final TestCase that = this;

				try {
					final MobileServicePush MobileServicePush = client.getPush();
					final TestResult result = new TestResult();
					result.setStatus(TestStatus.Passed);
					result.setTestCase(this);

					registerTemplate(this, MobileServicePush, UUID.randomUUID().toString(), templateName, (String[]) null, new TemplateRegistrationCallback() {

						@Override
						public void onRegister(TemplateRegistration templateRegistration, Exception exception) {
							if (exception != null) {
								callback.onTestComplete(that, createResultFromException(exception));
								return;
							}

							final String registrationId = templateRegistration.getRegistrationId();
							unregisterTemplate(that, MobileServicePush, templateName, new UnregisterTemplateCallback() {

								@Override
								public void onUnregister(Exception exception) {
									if (exception != null) {
										callback.onTestComplete(that, createResultFromException(exception));
										return;
									}

									addUnexistingTemplateRegistration(templateName, registrationId);

									unregisterTemplate(that, MobileServicePush, templateName, new UnregisterTemplateCallback() {

										@Override
										public void onUnregister(Exception exception) {
											if (exception != null) {
												callback.onTestComplete(that, createResultFromException(exception));
												return;
											}

											callback.onTestComplete(that, result);

											return;
										}
									});
								}
							});
						}
					});

				} catch (Exception e) {
					callback.onTestComplete(this, createResultFromException(e));
					return;
				}
			}
		};
		register.setName(name);
		register.setExpectedExceptionClass(MobileServiceException.class);

		return register;
	}

	// Unregister All Tests

	private TestCase createUnregisterAllUnregisterNativeTestCase(String name, final String templateName) {
		TestCase register = new TestCase() {

			@Override
			protected void executeTest(MobileServiceClient client, final TestExecutionCallback callback) {
				final TestCase that = this;

				try {
					final MobileServicePush MobileServicePush = client.getPush();
					final TestResult result = new TestResult();
					result.setStatus(TestStatus.Passed);
					result.setTestCase(this);

					final String gcmId = UUID.randomUUID().toString();

					unregisterAll(that, MobileServicePush, gcmId, new UnregisterAllCallback() {

						@Override
						public void onUnregister(Exception exception) {
							if (exception != null) {
								callback.onTestComplete(that, createResultFromException(exception));
								return;
							}

							register(that, MobileServicePush, gcmId, (String[]) null, new RegistrationCallback() {

								@Override
								public void onRegister(Registration nativeRegistration, Exception exception) {
									if (exception != null) {
										callback.onTestComplete(that, createResultFromException(exception));
										return;
									}

									final String registrationId = nativeRegistration.getRegistrationId();

									registerTemplate(that, MobileServicePush, gcmId, templateName, (String[]) null, new TemplateRegistrationCallback() {

										@Override
										public void onRegister(TemplateRegistration templateRegistration, Exception exception) {
											if (exception != null) {
												callback.onTestComplete(that, createResultFromException(exception));
												return;
											}

											unregisterAll(that, MobileServicePush, gcmId, new UnregisterAllCallback() {

												@Override
												public void onUnregister(Exception exception) {
													if (exception != null) {
														callback.onTestComplete(that, createResultFromException(exception));
														return;
													}

													addUnexistingNativeRegistration(registrationId);

													unregister(that, MobileServicePush, new UnregisterCallback() {

														@Override
														public void onUnregister(Exception exception) {
															if (exception != null) {
																callback.onTestComplete(that, createResultFromException(exception));
																return;
															}

															callback.onTestComplete(that, result);
															return;
														}
													});
												}

											});

										}
									});
								}
							});
						}
					});
				} catch (Exception e) {
					callback.onTestComplete(this, createResultFromException(e));
					return;
				}
			}
		};
		register.setName(name);
		register.setExpectedExceptionClass(MobileServiceException.class);

		return register;
	}

	private TestCase createUnregisterAllUnregisterTemplateTestCase(String name, final String templateName) {
		TestCase register = new TestCase() {

			@Override
			protected void executeTest(MobileServiceClient client, final TestExecutionCallback callback) {

				final TestCase that = this;

				try {
					final MobileServicePush MobileServicePush = client.getPush();
					final TestResult result = new TestResult();
					result.setStatus(TestStatus.Passed);
					result.setTestCase(this);

					final String gcmId = UUID.randomUUID().toString();

					unregisterAll(that, MobileServicePush, gcmId, new UnregisterAllCallback() {

						@Override
						public void onUnregister(Exception exception) {
							if (exception != null) {
								callback.onTestComplete(that, createResultFromException(exception));
								return;
							}

							register(that, MobileServicePush, gcmId, (String[]) null, new RegistrationCallback() {

								@Override
								public void onRegister(Registration nativeRegistration, Exception exception) {
									if (exception != null) {
										callback.onTestComplete(that, createResultFromException(exception));
										return;
									}

									registerTemplate(that, MobileServicePush, gcmId, templateName, (String[]) null, new TemplateRegistrationCallback() {

										@Override
										public void onRegister(TemplateRegistration templateRegistration, Exception exception) {
											if (exception != null) {
												callback.onTestComplete(that, createResultFromException(exception));
												return;
											}

											final String registrationId = templateRegistration.getRegistrationId();

											unregisterAll(that, MobileServicePush, gcmId, new UnregisterAllCallback() {

												@Override
												public void onUnregister(Exception exception) {
													if (exception != null) {
														callback.onTestComplete(that, createResultFromException(exception));
														return;
													}

													addUnexistingTemplateRegistration(templateName, registrationId);

													unregisterTemplate(that, MobileServicePush, templateName, new UnregisterTemplateCallback() {

														@Override
														public void onUnregister(Exception exception) {
															if (exception != null) {
																callback.onTestComplete(that, createResultFromException(exception));
																return;
															}

															callback.onTestComplete(that, result);
															return;
														}
													});

												}
											});

										}
									});

								}
							});

						}
					});
				} catch (Exception e) {
					callback.onTestComplete(this, createResultFromException(e));
					return;
				}
			}
		};
		register.setName(name);
		register.setExpectedExceptionClass(MobileServiceException.class);

		return register;
	}

	private TestCase createCheckIsRefreshNeeded(String name) {
		TestCase register = new TestCase() {

			@Override
			protected void executeTest(MobileServiceClient client, final TestExecutionCallback callback) {

				final TestCase that = this;

				try {
					final MobileServicePush MobileServicePush = client.getPush();
					final TestResult result = new TestResult();
					result.setStatus(TestStatus.Passed);
					result.setTestCase(this);

					final String gcmId = UUID.randomUUID().toString();

					unregisterAll(that, MobileServicePush, gcmId, new UnregisterAllCallback() {

						@Override
						public void onUnregister(Exception exception) {
							if (exception != null) {
								callback.onTestComplete(that, createResultFromException(exception));
								return;
							}

							register(that, MobileServicePush, gcmId, (String[]) null, new RegistrationCallback() {

								@Override
								public void onRegister(Registration nativeRegistration, Exception exception) {
									if (exception != null) {
										callback.onTestComplete(that, createResultFromException(exception));
										return;
									}

									registerTemplate(that, MobileServicePush, gcmId, UUID.randomUUID().toString(), (String[]) null,
											new TemplateRegistrationCallback() {

												@Override
												public void onRegister(TemplateRegistration templateRegistration, Exception exception) {
													if (exception != null) {
														callback.onTestComplete(that, createResultFromException(exception));
														return;
													}

													if (getRegistrationCountInLocalStorage() != 2) {
														result.setStatus(TestStatus.Failed);
													}

													callback.onTestComplete(that, result);
													return;
												}
											});
								}
							});
						}
					});
				} catch (Exception e) {
					callback.onTestComplete(this, createResultFromException(e));
					return;
				}
			}
		};

		register.setName(name);

		return register;
	}

	public EnhancedPushTests() {
		super("Enhanced Push tests");

		String[] noTags = (String[]) null;
		String[] oneTag = new String[] { "tagNum1" };
		String[] threeTags = new String[] { "tagNum1", "tagNum2", "tagNum3" };
		String[] otherTags = new String[] { "tagNum4", "tagNum5", "tagNum6", "tagNum7", "tagNum8" };
		String[] manyTags = new String[60];

		for (int i = 0; i < 60; i++) {
			manyTags[i] = "tagNum" + (i + 1);
		}

		this.addTest(createRegisterNativeTestCase("Register native - Register / Unregister - 60 tags", manyTags));
		this.addTest(createRegisterNativeTestCase("Register native - Register / Unregister - No tags", noTags));
		this.addTest(createRegisterNativeTestCase("Register native - Register / Unregister - One tag", oneTag));
		this.addTest(createRegisterNativeTestCase("Register native - Register / Unregister - Three tags", threeTags));

		this.addTest(createRegisterNativeTwiceTestCase("Register native - Register twice / Unregister - No tags -> Other tags", noTags, otherTags));
		this.addTest(createRegisterNativeTwiceTestCase("Register native - Register twice / Unregister - One tag -> Other tags", oneTag, otherTags));
		this.addTest(createRegisterNativeTwiceTestCase("Register native - Register twice / Unregister - Three tags -> Other tags", threeTags, otherTags));
		this.addTest(createRegisterNativeTwiceTestCase("Register native - Register twice / Unregister - Other tags -> No tags", otherTags, noTags));

		this.addTest(createReRegisterNativeTestCase("Register native - Register / Unregister / Register / Unregister"));

		this.addTest(createRegisterNativeEmptyGcmRegistrationIdTestCase("Register native - Empty GCM Registration Id"));

		this.addTest(createRegisterTemplateTestCase("Register template - Register / Unregister - 60 tags", UUID.randomUUID().toString(), manyTags));
		this.addTest(createRegisterTemplateTestCase("Register template - Register / Unregister - No tags", UUID.randomUUID().toString(), noTags));
		this.addTest(createRegisterTemplateTestCase("Register template - Register / Unregister - One tag", UUID.randomUUID().toString(), oneTag));
		this.addTest(createRegisterTemplateTestCase("Register template - Register / Unregister - Three tags", UUID.randomUUID().toString(), threeTags));

		this.addTest(createRegisterTemplateTwiceTestCase("Register template - Register twice / Unregister - No tags -> Other tags", UUID.randomUUID()
				.toString(), noTags, otherTags));
		this.addTest(createRegisterTemplateTwiceTestCase("Register template - Register twice / Unregister - One tag -> Other tags", UUID.randomUUID()
				.toString(), oneTag, otherTags));
		this.addTest(createRegisterTemplateTwiceTestCase("Register template - Register twice / Unregister - Three tags -> Other tags", UUID.randomUUID()
				.toString(), threeTags, otherTags));
		this.addTest(createRegisterTemplateTwiceTestCase("Register template - Register twice / Unregister - Other tags -> No tags", UUID.randomUUID()
				.toString(), otherTags, noTags));

		this.addTest(createRegisterTwoTemplatesTestCase("Register template - Register two templates / Unregister", UUID.randomUUID().toString(), UUID
				.randomUUID().toString()));

		this.addTest(createReRegisterTemplateTestCase("Register template - Register / Unregister / Register / Unregister", UUID.randomUUID().toString()));

		this.addTest(createRegisterTemplateEmptyGCMRegistrationIdTestCase("Register template - Empty GCM Registration Id", UUID.randomUUID().toString()));
		this.addTest(createRegisterTemplateEmptyTemplateNameTestCase("Register template - Empty template name"));
		this.addTest(createRegisterTemplateEmptyTemplateTestCase("Register template - Empty template", UUID.randomUUID().toString()));
		this.addTest(createReRegisterTemplateEmptyTemplateTestCase("Register template - Reregister Empty template", UUID.randomUUID().toString()));

		this.addTest(createUnregisterNativeNonExistingTestCase("Unregister native - Non existing"));
		this.addTest(createUnregisterNativeUnexistingRegistrationTestCase("Unregister native - Unexisting registration"));

		this.addTest(createUnregisterTemplateNonExistingTestCase("Unregister template - Non existing", UUID.randomUUID().toString()));
		this.addTest(createUnregisterTemplateUnexistingRegistrationTestCase("Unregister template - Unexisting registration", UUID.randomUUID().toString()));

		this.addTest(createUnregisterAllUnregisterNativeTestCase(
				"Unregister all - Register native / Register template / Unregister all / Unregister native - Unexisting registration", UUID.randomUUID()
						.toString()));
		this.addTest(createUnregisterAllUnregisterTemplateTestCase(
				"Unregister all - Register native / Register template / Unregister all / Unregister template - Unexisting registration", UUID.randomUUID()
						.toString()));

		this.addTest(createCheckIsRefreshNeeded("Retrieve existing registrations on first connection"));
	}
}