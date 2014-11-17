package org.auscope.portal.server.web.service.jobtask;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;

import junit.framework.Assert;

import org.auscope.eavl.wpsclient.ConditionalProbabilityWpsClient;
import org.auscope.portal.core.services.PortalServiceException;
import org.auscope.portal.core.services.cloud.FileStagingService;
import org.auscope.portal.core.test.PortalTestClass;
import org.auscope.portal.server.eavl.EAVLJob;
import org.auscope.portal.server.eavl.EAVLJobConstants;
import org.auscope.portal.server.web.service.CSVService;
import org.jmock.Expectations;
import org.junit.Test;

import com.google.common.collect.Sets;

public class TestImputationCallable extends PortalTestClass {
    private EAVLJob mockJob = context.mock(EAVLJob.class);
    private ConditionalProbabilityWpsClient mockWpsClient = context.mock(ConditionalProbabilityWpsClient.class);
    private CSVService mockCsvService = context.mock(CSVService.class);
    private FileStagingService mockFss = context.mock(FileStagingService.class);

    @Test
    public void testNormalOperation() throws Exception {
        ImputationCallable ic = new ImputationCallable(mockJob, mockWpsClient, mockCsvService, mockFss);

        final String holeIdParam = "hole-id";
        final String savedParam = "saved-param";

        final InputStream mockIs1 = context.mock(InputStream.class, "mockIs1");
        final OutputStream mockOs = context.mock(OutputStream.class);

        final Double[][] data = new Double[][] {{0.2, 0.4, null}};
        final double[][] imputedData = new double[][] {{0.2, 0.4, 0.9}};

        context.checking(new Expectations() {{
            allowing(mockFss).readFile(mockJob, EAVLJobConstants.FILE_DATA_CSV);will(returnValue(mockIs1));

            oneOf(mockFss).writeFile(mockJob, EAVLJobConstants.FILE_IMPUTED_CSV);will(returnValue(mockOs));

            oneOf(mockCsvService).getRawData(with(mockIs1), with(equal(Arrays.asList(1, 2))), with(false));will(returnValue(data));
            oneOf(mockCsvService).writeRawData(mockIs1, mockOs, imputedData);

            oneOf(mockWpsClient).imputationNA(data);will(returnValue(imputedData));

            allowing(mockJob).getHoleIdParameter();will(returnValue(holeIdParam));
            allowing(mockJob).getSavedParameters();will(returnValue(Sets.newHashSet(savedParam)));

            oneOf(mockCsvService).columnNameToIndex(mockIs1, savedParam);will(returnValue(new Integer(2)));
            oneOf(mockCsvService).columnNameToIndex(mockIs1, holeIdParam);will(returnValue(new Integer(1)));

            atLeast(1).of(mockIs1).close();
            oneOf(mockOs).close();
        }});

        Assert.assertSame(imputedData, ic.call());
    }

    @Test(expected=PortalServiceException.class)
    public void testWPSError() throws Exception {
        ImputationCallable ic = new ImputationCallable(mockJob, mockWpsClient, mockCsvService, mockFss);

        final InputStream mockIs1 = context.mock(InputStream.class, "mockIs1");

        final Double[][] data = new Double[][] {{0.2, 0.4, null}};
        final double[][] imputedData = new double[][] {{0.2, 0.4, 0.9}};

        context.checking(new Expectations() {{
            allowing(mockJob).getHoleIdParameter();will(returnValue("hole-id"));
            allowing(mockJob).getSavedParameters();will(returnValue(new HashSet<String>()));

            oneOf(mockCsvService).columnNameToIndex(mockIs1, "hole-id");will(returnValue(new Integer(1)));

            allowing(mockFss).readFile(mockJob, EAVLJobConstants.FILE_DATA_CSV);will(returnValue(mockIs1));
            oneOf(mockCsvService).getRawData(with(mockIs1), with(equal(Arrays.asList(1))), with(false));will(returnValue(data));
            oneOf(mockWpsClient).imputationNA(data);will(throwException(new IOException()));
            oneOf(mockIs1).close();
        }});

        Assert.assertSame(imputedData, ic.call());
    }
}
