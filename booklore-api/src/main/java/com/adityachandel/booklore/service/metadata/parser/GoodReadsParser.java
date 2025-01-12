package com.adityachandel.booklore.service.metadata.parser;

import com.adityachandel.booklore.model.dto.Book;
import com.adityachandel.booklore.service.metadata.model.FetchMetadataRequest;
import com.adityachandel.booklore.service.metadata.model.FetchedBookMetadata;
import com.adityachandel.booklore.service.metadata.model.MetadataProvider;
import com.adityachandel.booklore.util.BookUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    private static final String BASE_SEARCH_URL = "https://www.goodreads.com/search?q=";
    private static final String BASE_BOOK_URL = "https://www.goodreads.com/book/show/";
    private static final int COUNT_DETAILED_METADATA_TO_GET = 3;

    @Override
    public FetchedBookMetadata fetchTopMetadata(Book book, FetchMetadataRequest fetchMetadataRequest) {
        Optional<FetchedBookMetadata> preview = fetchMetadataPreviews(book, fetchMetadataRequest).stream().findFirst();
        if (preview.isEmpty()) {
            return null;
        }
        List<FetchedBookMetadata> fetchedMetadata = fetchMetadataUsingPreviews(List.of(preview.get()));
        return fetchedMetadata.isEmpty() ? null : fetchedMetadata.getFirst();
    }

    @Override
    public List<FetchedBookMetadata> fetchMetadata(Book book, FetchMetadataRequest fetchMetadataRequest) {
        List<FetchedBookMetadata> previews = fetchMetadataPreviews(book, fetchMetadataRequest).stream()
                .limit(COUNT_DETAILED_METADATA_TO_GET)
                .toList();
        return fetchMetadataUsingPreviews(previews);
    }

    private List<FetchedBookMetadata> fetchMetadataUsingPreviews(List<FetchedBookMetadata> previews) {
        List<FetchedBookMetadata> fetchedMetadata = new ArrayList<>();
        for (FetchedBookMetadata preview : previews) {
            log.info("GoodReads: Fetching metadata for: {}", preview.getTitle());
            try {
                Document document = fetchDoc(BASE_BOOK_URL + preview.getProviderBookId());
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
        builder.provider(MetadataProvider.GoodReads);
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
        String encodedSearchTerm = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8);
        return BASE_SEARCH_URL + encodedSearchTerm;
    }

    public List<FetchedBookMetadata> fetchMetadataPreviews(Book book, FetchMetadataRequest request) {
        String searchTerm = getSearchTerm(book, request);
        if (searchTerm != null) {
            log.info("GoodReads: Fetching metadata previews for: {}", searchTerm);
            try {
                String searchUrl = generateSearchUrl(searchTerm);
                Elements previewBooks = fetchDoc(searchUrl).select("table.tableList").first().select("tr[itemtype=http://schema.org/Book]");
                List<FetchedBookMetadata> metadataPreviews = new ArrayList<>();
                for (Element previewBook : previewBooks) {
                    FetchedBookMetadata previewMetadata = FetchedBookMetadata.builder()
                            .providerBookId(String.valueOf(extractGoodReadsIdPreview(previewBook)))
                            .title(extractTitlePreview(previewBook))
                            .authors(extractAuthorsPreview(previewBook))
                            .build();
                    metadataPreviews.add(previewMetadata);
                }
                Thread.sleep(Duration.ofSeconds(1));
                return metadataPreviews;
            } catch (Exception e) {
                log.error("Error fetching metadata previews: {}", e.getMessage());
                return Collections.emptyList();
            }
        }
        return Collections.emptyList();
    }

    private String getSearchTerm(Book book, FetchMetadataRequest request) {
        String searchTerm = (request.getTitle() != null && !request.getTitle().isEmpty())
                ? request.getTitle()
                : (book.getFileName() != null && !book.getFileName().isEmpty()
                ? BookUtils.cleanFileName(book.getFileName())
                : null);

        if (searchTerm != null) {
            searchTerm = searchTerm.replaceAll("[.,\\-\\[\\]{}()!@#$%^&*_=+|~`<>?/\";:]", "").trim();
            if (searchTerm.length() > 60) {
                String[] words = searchTerm.split("\\s+");
                StringBuilder truncated = new StringBuilder();
                for (String word : words) {
                    if (truncated.length() + word.length() + 1 > 60) break;
                    if (!truncated.isEmpty()) truncated.append(" ");
                    truncated.append(word);
                }
                searchTerm = truncated.toString();
            }
        }

        return searchTerm;
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