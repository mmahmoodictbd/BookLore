package com.adityachandel.booklore.service.metadata.parser;

import com.adityachandel.booklore.model.dto.Book;
import com.adityachandel.booklore.service.metadata.model.FetchMetadataRequest;
import com.adityachandel.booklore.service.metadata.model.FetchedBookMetadata;
import com.adityachandel.booklore.service.metadata.model.MetadataProvider;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class GoodReadsParser implements BookParser {

    private static final String SEARCH_BASE_URL = "https://www.goodreads.com/search?q=";
    private static final String BOOK_BASE_URL = "https://www.goodreads.com/book/show/";
    private static final int COUNT_DETAILED_METADATA_TO_GET = 3;

    @Override
    public FetchedBookMetadata fetchTopMetadata(Book book, FetchMetadataRequest fetchMetadataRequest) {
        Optional<FetchedBookMetadata> preview = fetchMetadataPreviews(fetchMetadataRequest).stream().findFirst();
        if (preview.isEmpty()) {
            return null;
        }
        List<FetchedBookMetadata> fetchedMetadata = fetchMetadataForPreviews(List.of(preview.get()));
        return fetchedMetadata.isEmpty() ? null : fetchedMetadata.getFirst();
    }

    @Override
    public List<FetchedBookMetadata> fetchMetadata(Book book, FetchMetadataRequest fetchMetadataRequest) {
        List<FetchedBookMetadata> previews = fetchMetadataPreviews(fetchMetadataRequest).stream()
                .limit(COUNT_DETAILED_METADATA_TO_GET)
                .toList();
        return fetchMetadataForPreviews(previews);
    }

    private List<FetchedBookMetadata> fetchMetadataForPreviews(List<FetchedBookMetadata> previews) {
        List<FetchedBookMetadata> fetchedMetadata = new ArrayList<>();
        for (FetchedBookMetadata preview : previews) {
            log.info("Fetching metadata for: {}", preview.getTitle());
            try {
                Document document = fetchDoc(BOOK_BASE_URL + preview.getProviderBookId());
                FetchedBookMetadata detailedMetadata = parseBookDetails(document, preview.getProviderBookId());
                if (detailedMetadata != null) {
                    fetchedMetadata.add(detailedMetadata);
                }
                Thread.sleep(Duration.ofSeconds(1));
            } catch (Exception e) {
                log.error("Error fetching metadata for book: {}", preview.getProviderBookId(), e);
            }
        }
        return fetchedMetadata;
    }

    private FetchedBookMetadata parseBookDetails(Document document, String providerBookId) {
        FetchedBookMetadata.FetchedBookMetadataBuilder builder = FetchedBookMetadata.builder().providerBookId(providerBookId);
        builder.provider(MetadataProvider.GOOD_READS);
        try {
            JSONObject apolloStateJson = getJson(document)
                    .getJSONObject("props")
                    .getJSONObject("pageProps")
                    .getJSONObject("apolloState");

            LinkedHashSet<String> keySet = getJsonKeys(apolloStateJson);

            extractContributorDetails(apolloStateJson, keySet, builder);
            extractBookDetails(apolloStateJson, keySet, builder);
            extractWorkDetails(apolloStateJson, keySet, builder);
        } catch (Exception e) {
            log.error("Error parsing book details for providerBookId: {}", providerBookId, e);
            return null;
        }

        return builder.build();
    }

    private void extractContributorDetails(JSONObject apolloStateJson, LinkedHashSet<String> keySet, FetchedBookMetadata.FetchedBookMetadataBuilder builder) {
        String contributorKey = findKeyByPrefix(keySet, "Contributor:kca");
        if (contributorKey != null) {
            String contributorName = getContributorName(apolloStateJson, contributorKey);
            if (contributorName != null) {
                builder.authors(List.of(contributorName));
            }
        }
    }

    private void extractBookDetails(JSONObject apolloStateJson, LinkedHashSet<String> keySet, FetchedBookMetadata.FetchedBookMetadataBuilder builder) {
        JSONObject bookJson = getValidBookJson(apolloStateJson, keySet, "Book:kca:");
        if (bookJson != null) {

            builder.title(handleStringNull(bookJson.optString("title")))
                    .description(handleStringNull(bookJson.optString("description")))
                    .thumbnailUrl(handleStringNull(bookJson.optString("imageUrl")))
                    .categories(extractGenres(bookJson));

            JSONObject detailsJson = bookJson.optJSONObject("details");
            if (detailsJson != null) {
                builder.asin(handleStringNull(detailsJson.optString("asin")))
                        .pageCount(parseInteger(detailsJson.optString("numPages")))
                        .publishedDate(convertToLocalDate(detailsJson.optString("publicationTime")))
                        .publisher(handleStringNull(detailsJson.optString("publisher")))
                        .isbn10(handleStringNull(detailsJson.optString("isbn")))
                        .isbn13(handleStringNull(detailsJson.optString("isbn13")));

                JSONObject languageJson = detailsJson.optJSONObject("language");
                if (languageJson != null) {
                    builder.language(handleStringNull(languageJson.optString("name")));
                }
            }
        }
    }

    private void extractWorkDetails(JSONObject apolloStateJson, LinkedHashSet<String> keySet, FetchedBookMetadata.FetchedBookMetadataBuilder builder) {
        String workKey = findKeyByPrefix(keySet, "Work:kca:");
        if (workKey != null) {
            JSONObject workJson = apolloStateJson.optJSONObject(workKey);
            if (workJson != null) {
                JSONObject statsJson = workJson.optJSONObject("stats");
                if (statsJson != null) {
                    builder.rating(parseDouble(statsJson.optString("averageRating")))
                            .ratingCount(parseInteger(statsJson.optString("ratingsCount")))
                            .reviewCount(parseInteger(statsJson.optString("textReviewsCount")));
                }
            }
        }
    }

    /*@Override
    public List<FetchedBookMetadata> fetchMetadata(Book book, FetchMetadataRequest fetchMetadataRequest) {
        List<FetchedBookMetadata> fetchedMetadata = new ArrayList<>();

        for (FetchedBookMetadata metadata : fetchMetadataPreviews(fetchMetadataRequest).stream().limit(COUNT_DETAILED_METADATA_TO_GET).toList()) {

            Document document = fetchDoc(BOOK_BASE_URL + metadata.getProviderBookId());
            FetchedBookMetadata.FetchedBookMetadataBuilder builder = FetchedBookMetadata.builder().providerBookId(metadata.getProviderBookId());

            try {
                JSONObject apolloStateJson = getJson(document).getJSONObject("props").getJSONObject("pageProps").getJSONObject("apolloState");
                LinkedHashSet<String> keySet = getJsonKeys(apolloStateJson);

                String contributorKey = findKeyByPrefix(keySet, "Contributor:kca");
                if (contributorKey != null) {
                    String contributorName = getContributorName(apolloStateJson, contributorKey);
                    if (contributorName != null) {
                        builder.authors(List.of(contributorName));
                    }
                }

                String bookKey = findKeyByPrefix(keySet, "Book:kca:");
                if (bookKey != null) {
                    JSONObject bookJson = apolloStateJson.getJSONObject(bookKey);
                    if (bookJson != null) {
                        builder.title(handleStringNull(bookJson.getString("title")));
                        builder.description(handleStringNull(bookJson.getString("description")));
                        builder.thumbnailUrl(handleStringNull(bookJson.getString("imageUrl")));
                        builder.categories(extractGenres(bookJson));

                        JSONObject detailsJson = bookJson.getJSONObject("details");
                        if (detailsJson != null) {
                            builder.asin(handleStringNull(detailsJson.getString("asin")));
                            builder.pageCount(parseInteger(detailsJson.getString("numPages")));
                            builder.publishedDate(convertToLocalDate(detailsJson.getString("publicationTime")));
                            builder.publisher(handleStringNull(detailsJson.getString("publisher")));
                            builder.isbn10(handleStringNull(detailsJson.getString("isbn")));
                            builder.isbn13(handleStringNull(detailsJson.getString("isbn13")));

                            JSONObject languageJsonObject = detailsJson.getJSONObject("language");
                            if (languageJsonObject != null) {
                                builder.language(handleStringNull(languageJsonObject.getString("name")));
                            }
                        }
                    }
                }

                String workKey = findKeyByPrefix(keySet, "Work:kca:");
                if (workKey != null) {
                    JSONObject workJson = apolloStateJson.getJSONObject(workKey);
                    if (workJson != null) {
                        JSONObject statsJson = workJson.getJSONObject("stats");
                        if (statsJson != null) {
                            builder.rating(parseDouble(statsJson.getString("averageRating")));
                            builder.ratingCount(parseInteger(statsJson.getString("ratingsCount")));
                            builder.reviewCount(parseInteger(statsJson.getString("textReviewsCount")));
                        }
                    }
                }
                Thread.sleep(Duration.ofSeconds(1));
            } catch (Exception e) {
                log.error("Error fetching metadata for book: {}", metadata.getProviderBookId(), e);
            }
            fetchedMetadata.add(builder.build());
        }
        return fetchedMetadata;
    }*/

    private Double parseDouble(String value) {
        try {
            return value != null ? Double.parseDouble(value) : null;
        } catch (NumberFormatException e) {
            log.error("Error parsing double: {}, Error: {}", value, e.getMessage());
            return null;
        }
    }

    private Integer parseInteger(String value) {
        try {
            return value != null ? Integer.parseInt(value) : null;
        } catch (NumberFormatException e) {
            log.error("Error parsing integer: {}, Error: {}", value, e.getMessage());
            return null;
        }
    }

    private String handleStringNull(String s) {
        if (s != null && s.equals("null")) {
            return null;
        }
        return s;
    }

    private LinkedHashSet<String> getJsonKeys(JSONObject apolloStateJson) {
        LinkedHashSet<String> keySet = new LinkedHashSet<>();
        Iterator<String> keys = apolloStateJson.keys();
        while (keys.hasNext()) {
            keySet.add(keys.next());
        }
        return keySet;
    }

    private JSONObject getValidBookJson(JSONObject apolloStateJson, LinkedHashSet<String> keySet, String prefix) {
        try {
            for (String key : keySet) {
                if (key.contains(prefix)) {
                    JSONObject bookJson = apolloStateJson.getJSONObject(key);
                    String string = bookJson.optString("title");
                    if (string != null && !string.isEmpty()) {
                        return bookJson;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error finding getValidBookJson: {}", e.getMessage());
        }
        return null;
    }

    private String findKeyByPrefix(LinkedHashSet<String> keySet, String prefix) {
        for (String key : keySet) {
            if (key.contains(prefix)) {
                return key;
            }
        }
        return null;
    }

    private String getContributorName(JSONObject apolloStateJson, String contributorKey) {
        try {
            if (contributorKey != null) {
                JSONObject contributorJson = apolloStateJson.getJSONObject(contributorKey);
                return contributorJson.getString("name");
            }
        } catch (Exception e) {
            log.error("Error fetching contributor name: {}, Error: {}", contributorKey, e.getMessage());
        }
        return null;
    }

    private List<String> extractGenres(JSONObject bookJson) {
        try {
            List<String> genres = new ArrayList<>();
            JSONArray bookGenresJsonArray = bookJson.getJSONArray("bookGenres");
            for (int i = 0; i < bookGenresJsonArray.length(); i++) {
                JSONObject genreJson = bookGenresJsonArray.getJSONObject(i).getJSONObject("genre");
                genres.add(genreJson.getString("name"));
            }
            return genres;
        } catch (Exception e) {
            log.error("Error extracting genres from book: {}, Error: {}", bookJson, e.getMessage());
        }
        return null;
    }

    private LocalDate convertToLocalDate(String timestamp) {
        try {
            long millis = Long.parseLong(timestamp);
            return Instant.ofEpochMilli(millis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        } catch (Exception e) {
            log.error("Invalid publication time: {}, Error: {}", timestamp, e.getMessage());
            return null;
        }
    }

    public JSONObject getJson(Element document) {
        try {
            Element scriptElement = document.getElementById("__NEXT_DATA__");

            if (scriptElement != null) {
                String jsonString = scriptElement.html();
                return new JSONObject(jsonString);
            } else {
                log.warn("No JSON script element found!");
            }
        } catch (Exception e) {
            log.error("No JSON script element found!", e);
        }
        return null;
    }

    public String generateSearchUrl(String searchTerm) {
        try {
            String encodedSearchTerm = URLEncoder.encode(searchTerm, "UTF-8");
            return SEARCH_BASE_URL + encodedSearchTerm;
        } catch (UnsupportedEncodingException e) {
            log.error("Error encoding search term: {}", searchTerm);
            return null;
        }
    }

    public List<FetchedBookMetadata> fetchMetadataPreviews(FetchMetadataRequest request) {
        log.info("Fetching metadata previews for {}", request.getTitle());
        try {
            String searchUrl = generateSearchUrl(request.getTitle() + " " + request.getAuthor());
            Elements books = fetchDoc(searchUrl).select("table.tableList").first().select("tr[itemtype=http://schema.org/Book]");
            List<FetchedBookMetadata> fetchedBookMetadataList = new ArrayList<>();
            for (Element book : books) {
                Integer publishedYear = extractPublishedYearPreview(book);
                FetchedBookMetadata metadata = FetchedBookMetadata.builder()
                        .providerBookId(String.valueOf(extractGoodReadsIdPreview(book)))
                        .title(extractTitlePreview(book))
                        .publishedDate(publishedYear != null ? LocalDate.of(publishedYear, 1, 1) : null)
                        .rating(extractRatingPreview(book))
                        .reviewCount(extractRatingsPreview(book))
                        .authors(extractAuthorsPreview(book))
                        .thumbnailUrl(extractCoverUrlPreview(book))
                        .build();
                fetchedBookMetadataList.add(metadata);
            }
            return fetchedBookMetadataList;
        } catch (Exception e) {
            log.error("Error fetching metadata previews: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private Integer extractGoodReadsIdPreview(Element book) {
        try {
            Element bookTitle = book.select("a.bookTitle").first();
            String href = bookTitle.attr("href");
            Pattern pattern = Pattern.compile("/book/show/(\\d+)");
            Matcher matcher = pattern.matcher(href);
            if (matcher.find()) {
                return Integer.valueOf(matcher.group(1));
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private List<String> extractAuthorsPreview(Element book) {
        List<String> authors = new ArrayList<>();
        try {
            Elements authorsElement = book.select("a.authorName");
            for (Element authorElement : authorsElement) {
                authors.add(authorElement.text());
            }
        } catch (Exception e) {
            log.warn("Error extracting author: {}", e.getMessage());
            return authors;
        }
        return authors;
    }

    private String extractTitlePreview(Element book) {
        try {
            Element link = book.select("a[title]").first();
            return link != null ? link.attr("title") : null;
        } catch (Exception e) {
            log.warn("Error extracting title: {}", e.getMessage());
            return null;
        }
    }

    private String extractCoverUrlPreview(Element book) {
        try {
            Element link = book.select("a[title]").first();
            return link != null ? link.select("img").attr("src") : null;
        } catch (Exception e) {
            log.warn("Error extracting image URL: {}", e.getMessage());
            return null;
        }
    }

    private Double extractRatingPreview(Element book) {
        try {
            Element ratingElement = book.selectFirst("span.greyText.smallText.uitext");
            if (ratingElement != null) {
                String[] split = ratingElement.text().split(" — ");
                String avgRating = split[0].split("avg rating")[0].trim();
                if (avgRating.length() <= 4) {
                    return Double.valueOf(avgRating);
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("Error extracting rating: {}", e.getMessage());
            return null;
        }
    }

    private Integer extractRatingsPreview(Element book) {
        try {
            Element ratingElement = book.selectFirst("span.greyText.smallText.uitext");
            if (ratingElement != null) {
                String[] split = ratingElement.text().split(" — ");
                if (split.length > 1) {
                    return Integer.parseInt(split[1].split("ratings")[0].replace(",", "").trim());
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("Error extracting number of ratings: {}", e.getMessage());
            return null;
        }
    }

    private Integer extractPublishedYearPreview(Element book) {
        try {
            Element ratingElement = book.selectFirst("span.greyText.smallText.uitext");
            if (ratingElement != null) {
                String[] split = ratingElement.text().split(" — ");
                if (split.length == 4) {
                    return Integer.parseInt(split[2].split("published")[1].trim());
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("Error extracting published year: {}", e.getMessage());
            return null;
        }
    }

    private Document fetchDoc(String url) {
        try {
            Connection.Response response = Jsoup.connect(url)
                    .header("accept", "text/html, application/json")
                    .header("accept-language", "en-US,en;q=0.9")
                    .header("content-type", "application/json")
                    .header("device-memory", "8")
                    .header("downlink", "10")
                    .header("dpr", "2")
                    .header("ect", "4g")
                    .header("origin", "https://www.amazon.com")
                    .header("priority", "u=1, i")
                    .header("rtt", "50")
                    .header("sec-ch-device-memory", "8")
                    .header("sec-ch-dpr", "2")
                    .header("sec-ch-ua", "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"")
                    .header("sec-ch-ua-mobile", "?0")
                    .header("sec-ch-ua-platform", "\"macOS\"")
                    .header("sec-ch-viewport-width", "1170")
                    .header("sec-fetch-dest", "empty")
                    .header("sec-fetch-mode", "cors")
                    .header("sec-fetch-site", "same-origin")
                    .header("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                    .header("viewport-width", "1170")
                    .header("x-amz-amabot-click-attributes", "disable")
                    .header("x-requested-with", "XMLHttpRequest")
                    .method(Connection.Method.GET)
                    .execute();
            return response.parse();
        } catch (IOException e) {
            log.error("Error parsing url: {}", url, e);
            throw new RuntimeException(e);
        }
    }
}