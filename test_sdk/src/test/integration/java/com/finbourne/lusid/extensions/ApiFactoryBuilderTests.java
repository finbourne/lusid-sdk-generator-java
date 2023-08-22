package com.finbourne.lusid.extensions;

import com.finbourne.lusid.ApiException;
import com.finbourne.lusid.api.InstrumentsApi;
import com.finbourne.lusid.model.ResourceListOfInstrumentIdTypeDescriptor;
import com.finbourne.lusid.extensions.ApiConfigurationException;
import com.finbourne.lusid.extensions.ApiFactory;
import com.finbourne.lusid.extensions.ApiFactoryBuilder;
import com.finbourne.lusid.extensions.auth.FinbourneTokenException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;

public class ApiFactoryBuilderTests {

        @Rule
        public ExpectedException thrown = ExpectedException.none();

        @Test
        public void build_WithExistingConfigurationFile_ShouldReturnFactory()
                        throws ApiException, ApiConfigurationException, FinbourneTokenException {
                ApiFactory ApiFactory = ApiFactoryBuilder.build(CredentialsSource.credentialsFile);
                assertThat(ApiFactory, is(notNullValue()));
                assertThatFactoryBuiltApiCanMakeLUSIDCalls(ApiFactory);
        }

        private static void assertThatFactoryBuiltApiCanMakeLUSIDCalls(ApiFactory ApiFactory)
                        throws ApiException {
                InstrumentsApi instrumentsApi = ApiFactory.build(InstrumentsApi.class);
                ResourceListOfInstrumentIdTypeDescriptor instrumentIdTypeDescriptor = instrumentsApi
                                .getInstrumentIdentifierTypes();
                assertThat("Instruments API created by factory should have returned instrument identifier types",
                                instrumentIdTypeDescriptor, is(notNullValue()));
                assertThat("Instrument identifier types returned by the Instrument API should not be empty",
                                instrumentIdTypeDescriptor.getValues(), not(empty()));
        }

}
