package dbms;

import dbms.models.*;
import dbms.services.DataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import javafx.application.Application;
import java.util.List;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;

public class App extends Application {
    static {
        // Critical Fix for Direct3D rendering crashes (D3DTextureData NullPointerException)
        // This ensures software rendering is chosen before JavaFX initializes.
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.verbose", "true");
        System.out.println("App: Forced Software Rendering Pipeline.");
    }
    private final DataService dataService = new DataService();
    private final ObjectMapper mapper = new ObjectMapper();
    private WebEngine webEngine;
    private JSGateway bridge;

    @Override
    public void start(Stage stage) {
        WebView webView = new WebView();
        this.webEngine = webView.getEngine();
        this.bridge = new JSGateway();

        // Custom Context Menu for Inspect Element (Eruda DevTools)
        webView.setContextMenuEnabled(false);
        ContextMenu contextMenu = new ContextMenu();
        MenuItem inspectItem = new MenuItem("Inspect Element");
        inspectItem.setOnAction(e -> {
            webEngine.executeScript("if(!document.getElementById('eruda-plugin')){var script=document.createElement('script');script.id='eruda-plugin';script.src='https://cdn.jsdelivr.net/npm/eruda';document.body.appendChild(script);script.onload=function(){eruda.init();};}");
        });
        contextMenu.getItems().add(inspectItem);

        webView.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(webView, e.getScreenX(), e.getScreenY());
            } else {
                contextMenu.hide();
            }
        });

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaApp", bridge);
                
                webEngine.executeScript(
                    "console.log = function(message) { window.javaApp.log(String(message)); }; " +
                    "console.error = function(message) { window.javaApp.log('JS_ERROR: ' + String(message)); }; " +
                    "window.onerror = function(msg, url, line) { window.javaApp.log('GLOBAL_ERROR: ' + msg + ' at ' + url + ':' + line); };"
                );
                System.out.println("Bridge successfully loaded.");
                webEngine.executeScript("if(typeof window.populateYearSelectsFromJava === 'function') { javaApp.getYearRange('window.populateYearSelectsFromJava'); }");
            }
        });

        String url = getClass().getResource("/index.html").toExternalForm();
        webEngine.load(url);

        Scene scene = new Scene(webView, 1200, 800);
        stage.setTitle("DBMS Bibliographic Data Explorer");
        stage.setScene(scene);
        stage.show();
    }

    private void runJS(String script) {
        javafx.application.Platform.runLater(() -> {
            try {
                webEngine.executeScript(script);
            } catch (Exception e) {
                System.err.println("Error executing JS: " + e.getMessage());
            }
        });
    }

    public class JSGateway {
        public void getUnifiedProfile(String name, String callback) {
            new Thread(() -> {
                try {
                    Object profile = dataService.getUnifiedProfile(name);
                    if (profile == null) {
                        runJS(callback + "({\"error\": \"Δεν βρέθηκε ακριβές προφίλ για το όνομα: " + name + "\"})");
                    } else {
                        String json = mapper.writeValueAsString(profile);
                        runJS(callback + "(" + json + ")");
                    }
                } catch (Exception e) {
                    runJS(callback + "({\"error\": \"Σφάλμα συστήματος: " + e.getMessage() + "\"})");
                }
            }).start();
        }

        public void getAuthorProfile(String name, String callback) {
            new Thread(() -> {
                try {
                    String json = mapper.writeValueAsString(dataService.getAuthorProfile(name));
                    runJS(callback + "(" + json + ")");
                } catch (Exception e) {
                    runJS(callback + "({\"error\": \"Σφάλμα ανάκτησης προφίλ συγγραφέα: " + e.getMessage() + "\"})");
                }
            }).start();
        }

        public void getConferenceProfile(String name, String callback) {
            new Thread(() -> {
                try {
                    String json = mapper.writeValueAsString(dataService.getConferenceProfile(name));
                    runJS(callback + "(" + json + ")");
                } catch (Exception e) {
                    runJS(callback + "({\"error\": \"Σφάλμα ανάκτησης προφίλ συνεδρίου: " + e.getMessage() + "\"})");
                }
            }).start();
        }

        public void getJournalProfile(String name, String callback) {
            new Thread(() -> {
                try {
                    String json = mapper.writeValueAsString(dataService.getJournalProfile(name));
                    runJS(callback + "(" + json + ")");
                } catch (Exception e) {
                    runJS(callback + "({\"error\": \"Σφάλμα ανάκτησης προφίλ περιοδικού: " + e.getMessage() + "\"})");
                }
            }).start();
        }

        public void getPaperDetails(String type, String value, int limit, int offset, String minYearStr, String maxYearStr, String keyword, String sortField, String sortOrder, String callback) {
            new Thread(() -> {
                try {
                    System.out.println("JS Log: Requesting papers for " + type + ": " + value + " (offset: " + offset + ")");
                    Integer minYear = (minYearStr == null || minYearStr.trim().isEmpty()) ? null : Integer.parseInt(minYearStr.trim());
                    Integer maxYear = (maxYearStr == null || maxYearStr.trim().isEmpty()) ? null : Integer.parseInt(maxYearStr.trim());
                    PaginatedResult<PaperDetail> result = dataService.getPaperDetails(type, value, limit, offset, minYear, maxYear, keyword, sortField, sortOrder);
                    
                    System.out.println("JS Log: Retrieved " + result.data.size() + " papers out of " + result.totalCount + " total.");
                    
                    String json = mapper.writeValueAsString(result);
                    runJS(callback + "(" + json + ")");
                } catch (Exception e) {
                    System.err.println("JS Log: Error in getPaperDetails: " + e.getMessage());
                    runJS(callback + "({\"data\":[], \"totalCount\":0, \"pageSize\":" + limit + ", \"currentPage\":1})");
                }
            }).start();
        }

        public void getYearProfile(int year, String callback) {
            new Thread(() -> {
                try {
                    String json = mapper.writeValueAsString(dataService.getYearProfile(year));
                    runJS(callback + "(" + json + ")");
                } catch (Exception e) {
                    runJS(callback + "(null)");
                }
            }).start();
        }

        public void getCategoryTrend(String categoryName, String callback) {
            new Thread(() -> {
                try {
                    String json = mapper.writeValueAsString(dataService.getCategoryTrend(categoryName));
                    runJS(callback + "(" + json + ")");
                } catch (Exception e) {
                    runJS(callback + "([])");
                }
            }).start();
        }

        public void getTopAuthors(int limit, int offset, String callback) {
            new Thread(() -> {
                try {
                    String json = mapper.writeValueAsString(dataService.getTopAuthors(limit, offset));
                    runJS(callback + "(" + json + ")");
                } catch (Exception e) {
                    runJS(callback + "([])");
                }
            }).start();
        }

        public void getPublicationsPerYear(String filter, int from, int to, String callback) {
            new Thread(() -> {
                try {
                    String json = mapper.writeValueAsString(dataService.getPublicationsPerYear(filter, from, to));
                    runJS(callback + "(" + json + ")");
                } catch (Exception e) {
                    runJS(callback + "([])");
                }
            }).start();
        }

        public void getScatterData(int from, int to, String callback) {
            new Thread(() -> {
                try {
                    String json = mapper.writeValueAsString(dataService.getScatterData(from, to));
                    runJS(callback + "(" + json + ")");
                } catch (Exception e) {
                    runJS(callback + "([])");
                }
            }).start();
        }

        public void getScatterAuthorsVsPapersStream(int from, int to, String callback) {
            new Thread(() -> {
                try {
                    int step = 2; // Process 2 years at a time to keep UI highly responsive
                    for (int current = from; current <= to; current += step) {
                        int currentTo = Math.min(current + step - 1, to);
                        List<ChartPoint> chunk = dataService.getScatterAuthorsVsPapers(current, currentTo);
                        if (!chunk.isEmpty()) {
                            String json = mapper.writeValueAsString(chunk);
                            runJS(callback + "(" + json + ", false)");
                        }
                    }
                    runJS(callback + "([], true)");
                } catch (Exception e) {
                    runJS(callback + "([], true)");
                }
            }).start();
        }

        public void getPublisherStats(String callback) {
            new Thread(() -> {
                try {
                    String json = mapper.writeValueAsString(dataService.getPublisherStats());
                    runJS(callback + "(" + json + ")");
                } catch (Exception e) {
                    runJS(callback + "([])");
                }
            }).start();
        }

        public void searchVenuesFast(String query, String callback) {
            new Thread(() -> {
                try {
                    String json = mapper.writeValueAsString(dataService.searchVenuesFast(query));
                    runJS(callback + "(" + json + ")");
                } catch (Exception e) {
                    runJS(callback + "([])");
                }
            }).start();
        }

        public void searchEntities(String query, String callback) {
            new Thread(() -> {
                try {
                    String json = mapper.writeValueAsString(dataService.searchEntitiesDetailed(query));
                    runJS(callback + "(" + json + ")");
                } catch (Exception e) {
                    runJS(callback + "([])");
                }
            }).start();
        }

        public void getYearRange(String callback) {
            new Thread(() -> {
                try {
                    int[] range = dataService.getYearRange();
                    String json = mapper.writeValueAsString(range);
                    runJS(callback + "(" + json + ")");
                } catch (Exception e) {
                    runJS(callback + "([1900, 2026])");
                }
            }).start();
        }

        public void getVenuesComparisonData(String venueNamesJson, String metric, int fromYear, int toYear, String callback) {
            new Thread(() -> {
                try {
                    List<String> venueNames = mapper.readValue(venueNamesJson, new TypeReference<List<String>>(){});
                    String json = mapper.writeValueAsString(dataService.getVenuesComparisonData(venueNames, metric, fromYear, toYear));
                    runJS(callback + "(" + json + ")");
                } catch (Exception e) {
                    runJS(callback + "([])");
                }
            }).start();
        }

        public void log(String message) {
            System.out.println("JS Log: " + message);
        }

        public void openInBrowser(String url) {
            if (java.awt.Desktop.isDesktopSupported()) {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
