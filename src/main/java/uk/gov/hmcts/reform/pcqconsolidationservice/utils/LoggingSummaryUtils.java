package uk.gov.hmcts.reform.pcqconsolidationservice.utils;

import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public final class LoggingSummaryUtils {

    private static final String CR_STRING = "\r\n";
    private static final String TAB_STRING = "| ";
    private static final String TOTAL_ONLINE_STRING = "Total Online";
    private static final String TOTAL_PAPER_STRING = "Total Paper";
    private static final String TOTAL_STRING = "Total";
    private static final String SUMMARY_HEADING_STRING = "\r\nConsolidation Service Case Matching Summary : ";
    private static final String FORMAT_STR_LENGTH_40 = "%1$-40s";
    private static final String SERVICE_SUMMARY_STRING = String.format(FORMAT_STR_LENGTH_40, "Service")
            + "Matched | Not Found | Errors\r\n";
    private static final String ONLINE_CHANNEL_SUFFIX = " Online Channel";
    private static final String ONLINE_MATCH_SUFFIX = "_online_channel_matched";
    private static final String ONLINE_NOT_FOUND_SUFFIX = "_online_channel_not_found";
    private static final String ONLINE_ERROR_SUFFIX = "_online_channel_error";
    private static final String PAPER_CHANNEL_SUFFIX = " Paper Channel";
    private static final String PAPER_MATCH_SUFFIX = "_paper_channel_matched";
    private static final String PAPER_NOT_FOUND_SUFFIX = "_paper_channel_not_found";
    private static final String PAPER_ERROR_SUFFIX = "_paper_channel_error";
    private static final String FORMAT_STR_LENGTH_10 = "%1$-10s";
    private static final String FORMAT_STR_LENGTH_8 = "%1$-8s";
    private static Set<String> serviceKeySet;

    private LoggingSummaryUtils() {
        //Private No Args Constructor
    }

    public static void logSummary(Map<String, Integer> serviceSummaryMap, Set<String> serviceKeySet) {
        StringBuilder stringBuilder = new StringBuilder(getSummaryString());
        LoggingSummaryUtils.serviceKeySet = serviceKeySet;

        AtomicInteger totalOnlineMatched = new AtomicInteger();
        AtomicInteger totalOnlineNotFound = new AtomicInteger();
        AtomicInteger totalOnlineError = new AtomicInteger();
        AtomicInteger totalPaperMatched = new AtomicInteger();
        AtomicInteger totalPaperNotFound = new AtomicInteger();
        AtomicInteger totalPaperError = new AtomicInteger();

        stringBuilder.append(getServiceSummaryString(totalOnlineMatched, totalOnlineNotFound, totalOnlineError,
                totalPaperMatched, totalPaperNotFound, totalPaperError, serviceSummaryMap))
                .append(String.format(FORMAT_STR_LENGTH_40,TOTAL_ONLINE_STRING))
                .append(String.format(FORMAT_STR_LENGTH_8,totalOnlineMatched.intValue()))
                .append(TAB_STRING)
                .append(String.format(FORMAT_STR_LENGTH_10,totalOnlineNotFound.intValue()))
                .append(TAB_STRING)
                .append(totalOnlineError.intValue())
                .append(CR_STRING)
                .append(String.format(FORMAT_STR_LENGTH_40,TOTAL_PAPER_STRING))
                .append(String.format(FORMAT_STR_LENGTH_8,totalPaperMatched.intValue()))
                .append(TAB_STRING)
                .append(String.format(FORMAT_STR_LENGTH_10,totalPaperNotFound.intValue()))
                .append(TAB_STRING)
                .append(totalPaperError.intValue())
                .append(CR_STRING)
                .append(String.format(FORMAT_STR_LENGTH_40,TOTAL_STRING))
                .append(String.format(FORMAT_STR_LENGTH_8,totalOnlineMatched.intValue() + totalPaperMatched.intValue()))
                .append(TAB_STRING)
                .append(String.format(FORMAT_STR_LENGTH_10,totalOnlineNotFound.intValue()
                        + totalPaperNotFound.intValue()))
                .append(TAB_STRING)
                .append(totalOnlineError.intValue() + totalPaperError.intValue());

        log.info(stringBuilder.toString());
    }

    private static String getSummaryString() {
        StringBuilder stringBuilder = new StringBuilder(SUMMARY_HEADING_STRING);
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd MMMMM yyyy", Locale.UK);
        stringBuilder.append(dateFormat.format(new Date()))
                .append(CR_STRING)
                .append(SERVICE_SUMMARY_STRING)
                .append("-----------------------------------------------------------")
                .append(CR_STRING);
        return stringBuilder.toString();
    }

    private static String getServiceSummaryString(AtomicInteger totalOnlineMatched, AtomicInteger totalOnlineNotFound,
                                           AtomicInteger totalOnlineError, AtomicInteger totalPaperMatched,
                                           AtomicInteger totalPaperNotFound, AtomicInteger totalPaperError,
                                                  Map<String, Integer> serviceSummaryMap) {

        StringBuilder stringBuilder = new StringBuilder();

        serviceKeySet.forEach(service -> {
            String serviceKey = service.toLowerCase(Locale.UK);
            stringBuilder.append(String.format(FORMAT_STR_LENGTH_40,service.toUpperCase(Locale.UK)
                    + ONLINE_CHANNEL_SUFFIX));
            Integer onlineMatchedCount = serviceSummaryMap.get(serviceKey + ONLINE_MATCH_SUFFIX);
            Integer onlineNotFoundCount =  serviceSummaryMap.get(serviceKey + ONLINE_NOT_FOUND_SUFFIX);
            Integer onlineErredCount = serviceSummaryMap.get(serviceKey + ONLINE_ERROR_SUFFIX);
            stringBuilder.append(countsString(onlineMatchedCount, onlineNotFoundCount, onlineErredCount))
                    .append(String.format(FORMAT_STR_LENGTH_40,service.toUpperCase(Locale.UK)
                            + PAPER_CHANNEL_SUFFIX));
            Integer paperMatchedCount = serviceSummaryMap.get(serviceKey + PAPER_MATCH_SUFFIX);
            Integer paperNotFoundCount =  serviceSummaryMap.get(serviceKey + PAPER_NOT_FOUND_SUFFIX);
            Integer paperErredCount = serviceSummaryMap.get(serviceKey + PAPER_ERROR_SUFFIX);
            stringBuilder.append(countsString(paperMatchedCount, paperNotFoundCount, paperErredCount));
            totalOnlineMatched.addAndGet(onlineMatchedCount == null ? 0 : onlineMatchedCount);
            totalOnlineNotFound.addAndGet(onlineNotFoundCount == null ? 0 : onlineNotFoundCount);
            totalOnlineError.addAndGet(onlineErredCount == null ? 0 : onlineErredCount);
            totalPaperMatched.addAndGet(paperMatchedCount == null ? 0 : paperMatchedCount);
            totalPaperNotFound.addAndGet(paperNotFoundCount == null ? 0 : paperNotFoundCount);
            totalPaperError.addAndGet(paperErredCount == null ? 0 : paperErredCount);
        });

        return stringBuilder.toString();
    }

    private static String countsString(Integer matchedCount, Integer notFoundCount, Integer erredCount) {

        return String.format(FORMAT_STR_LENGTH_8, matchedCount == null ? 0 : matchedCount)
                + TAB_STRING
                + String.format(FORMAT_STR_LENGTH_10, notFoundCount == null ? 0 : notFoundCount)
                + TAB_STRING
                + (erredCount == null ? 0 : erredCount)
                + CR_STRING;
    }
}
