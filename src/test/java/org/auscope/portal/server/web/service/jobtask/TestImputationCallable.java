package org.auscope.portal.server.web.service.jobtask;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import junit.framework.Assert;

import org.auscope.portal.core.services.PortalServiceException;
import org.auscope.portal.core.services.cloud.FileStagingService;
import org.auscope.portal.core.test.PortalTestClass;
import org.auscope.portal.server.eavl.EAVLJob;
import org.auscope.portal.server.eavl.EAVLJobConstants;
import org.auscope.portal.server.web.service.CSVService;
import org.auscope.portal.server.web.service.EAVLJobService;
import org.auscope.portal.server.web.service.WpsService;
import org.auscope.portal.server.web.service.wps.WpsServiceClient;
import org.jmock.Expectations;
import org.junit.Test;

import com.google.common.collect.Sets;

public class TestImputationCallable extends PortalTestClass {
    private EAVLJob mockJob = context.mock(EAVLJob.class, "mockJob");
    private EAVLJob mockJob2 = context.mock(EAVLJob.class, "mockJob2");
    private WpsService mockWpsClient = context.mock(WpsService.class);
    private CSVService mockCsvService = context.mock(CSVService.class);
    private FileStagingService mockFss = context.mock(FileStagingService.class);
    private EAVLJobService mockJobService = context.mock(EAVLJobService.class);

    @Test
    public void testNoScalingOperation() throws Exception {
        ImputationCallable ic = new ImputationCallable(mockJob, mockWpsClient, mockCsvService, mockFss, mockJobService, null, null, null);

        final String holeIdParam = "hole-id";
        final String savedParam = "saved-param";
        final String predictionParam = "param-to-predict";

        final InputStream mockIs1 = context.mock(InputStream.class, "mockIs1");
        final InputStream mockIs2 = context.mock(InputStream.class, "mockIs2");
        final InputStream mockIs3 = context.mock(InputStream.class, "mockIs3");
        final OutputStream mockOs = context.mock(OutputStream.class, "mockOs");
        final OutputStream mockOsTmp = context.mock(OutputStream.class, "mockOsTmp");
        final OutputStream mockOsValidate = context.mock(OutputStream.class, "mockOsValidate");
        final WpsServiceClient mockWpsServiceClient = context.mock(WpsServiceClient.class);

        final double[][] data = new double[][] {{0.2, 0.4, Double.NaN}};
        final double[][] imputedData = new double[][] {{0.2, 0.4, 0.9}};

        context.checking(new Expectations() {{
            allowing(mockFss).readFile(mockJob, EAVLJobConstants.FILE_DATA_CSV);will(returnValue(mockIs1));
            allowing(mockFss).readFile(mockJob, EAVLJobConstants.FILE_TEMP_DATA_CSV);will(returnValue(mockIs2));
            allowing(mockFss).readFile(mockJob, EAVLJobConstants.FILE_VALIDATED_DATA_CSV);will(returnValue(mockIs3));

            oneOf(mockFss).writeFile(mockJob, EAVLJobConstants.FILE_TEMP_DATA_CSV);will(returnValue(mockOsTmp));
            oneOf(mockFss).writeFile(mockJob, EAVLJobConstants.FILE_IMPUTED_CSV);will(returnValue(mockOs));
            oneOf(mockFss).writeFile(mockJob, EAVLJobConstants.FILE_VALIDATED_DATA_CSV);will(returnValue(mockOsValidate));
            oneOf(mockFss).renameStageInFile(mockJob, EAVLJobConstants.FILE_IMPUTED_CSV, EAVLJobConstants.FILE_IMPUTED_SCALED_CSV);

            oneOf(mockCsvService).getRawData(with(mockIs3), with(equal(Arrays.asList(2, 1, 3))), with(false));will(returnValue(data));
            oneOf(mockCsvService).writeRawData(with(mockIs3), with(mockOsTmp), with(imputedData), with(equal(Arrays.asList(2, 1, 3))), with(false));
            oneOf(mockCsvService).mergeFiles(with(mockIs3), with(mockIs2), with(mockOs), with(equal(Arrays.asList(2, 1, 3))), with(((List<Integer>) null)));

            oneOf(mockFss).deleteStageInFile(mockJob, EAVLJobConstants.FILE_TEMP_DATA_CSV);

            oneOf(mockWpsClient).getWpsClient();will(returnValue(mockWpsServiceClient));
            oneOf(mockWpsServiceClient).imputationNA(data);will(returnValue(imputedData));

            allowing(mockJob).getHoleIdParameter();will(returnValue(holeIdParam));
            allowing(mockJob).getSavedParameters();will(returnValue(Sets.newHashSet(savedParam)));
            allowing(mockJob).getPredictionParameter();will(returnValue(predictionParam));

            oneOf(mockCsvService).columnNameToIndex(mockIs1, holeIdParam);will(returnValue(new Integer(1)));
            oneOf(mockCsvService).columnNameToIndex(mockIs1, predictionParam);will(returnValue(new Integer(3)));
            oneOf(mockCsvService).columnNameToIndex(with(mockIs1), with(Arrays.asList(savedParam)));will(returnValue(Arrays.asList(new Integer(2))));

            oneOf(mockCsvService).cullEmptyRows(mockIs1, mockOsValidate, Arrays.asList(2, 1, 3), false);

            atLeast(1).of(mockIs2).close();
            atLeast(1).of(mockIs3).close();
            atLeast(1).of(mockOs).close();
        }});

        Assert.assertSame(imputedData, ic.call());
    }

    @Test(expected=PortalServiceException.class)
    public void testWPSError() throws Exception {
        ImputationCallable ic = new ImputationCallable(mockJob, mockWpsClient, mockCsvService, mockFss, mockJobService, null, null, null);

        final String predictionParam = "param-to-predict";

        final InputStream mockIs1 = context.mock(InputStream.class, "mockIs1");
        final OutputStream mockOs = context.mock(OutputStream.class, "mockOs");

        final double[][] data = new double[][] {{0.2, 0.4, Double.NaN}};
        final double[][] imputedData = new double[][] {{0.2, 0.4, 0.9}};

        context.checking(new Expectations() {{
            allowing(mockJob).getId();will(returnValue(999));
            allowing(mockJob).getHoleIdParameter();will(returnValue("hole-id"));
            allowing(mockJob).getSavedParameters();will(returnValue(new HashSet<String>()));
            allowing(mockJob).getPredictionParameter();will(returnValue(predictionParam));

            oneOf(mockCsvService).columnNameToIndex(mockIs1, "hole-id");will(returnValue(new Integer(1)));
            oneOf(mockCsvService).columnNameToIndex(mockIs1, predictionParam);will(returnValue(new Integer(3)));
            oneOf(mockCsvService).columnNameToIndex(with(mockIs1), with(new ArrayList<String>()));will(returnValue(new ArrayList<Integer>()));

            allowing(mockFss).readFile(mockJob, EAVLJobConstants.FILE_DATA_CSV);will(returnValue(mockIs1));
            allowing(mockFss).readFile(mockJob, EAVLJobConstants.FILE_VALIDATED_DATA_CSV);will(returnValue(mockIs1));
            oneOf(mockFss).writeFile(mockJob, EAVLJobConstants.FILE_VALIDATED_DATA_CSV);will(returnValue(mockOs));

            oneOf(mockCsvService).getRawData(with(mockIs1), with(equal(Arrays.asList(1, 3))), with(false));will(returnValue(data));
            oneOf(mockWpsClient).getWpsClient().imputationNA(data);will(throwException(new PortalServiceException("")));
            oneOf(mockIs1).close();

            oneOf(mockCsvService).cullEmptyRows(mockIs1, mockOs, Arrays.asList(1, 3), false);

            oneOf(mockOs).close();

            oneOf(mockJobService).getJobById(999);will(returnValue(mockJob2));
            oneOf(mockJob2).setImputationTaskError(with(any(String.class)));
            oneOf(mockJobService).save(mockJob2);
        }});

        Assert.assertSame(imputedData, ic.call());
    }
}
