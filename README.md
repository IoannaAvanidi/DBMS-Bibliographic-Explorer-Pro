# DBMS Bibliographic Explorer Pro 🔍📊

## 👥 Συντελεστές (Contributors)
* **Κοκκίνης Ιωάννης** (ΑΜ: **5109**)
* **Αβανίδη Ιωάννα-Μαρία** (ΑΜ: **4977**)

---

## 📽️ Βίντεο Παρουσίασης & Επίδειξης (Presentation Video)
Μπορείτε να παρακολουθήσετε το βίντεο παρουσίασης του συστήματος (διάρκειας ~15 λεπτών) απευθείας παρακάτω:

<video src="deliverables/%CF%80%CE%B8%CF%84%CE%B5%CE%B2%CE%B4_final.mp4" width="100%" controls>
  Το πρόγραμμα περιήγησής σας δεν υποστηρίζει την ετικέτα βίντεο. Μπορείτε να το κατεβάσετε ή να το δείτε απευθείας από εδώ: 
  <a href="deliverables/%CF%80%CE%B8%CF%84%CE%B5%CE%B2%CE%B4_final.mp4">deliverables/πθτεβδ_final.mp4</a>
</video>

> 🔗 **Direct Link:** [deliverables/πθτεβδ_final.mp4](deliverables/%CF%80%CE%B8%CF%84%CE%B5%CE%B2%CE%B4_final.mp4)

---

## 🏛️ Αρχιτεκτονική & Τεχνολογίες (System Architecture)
Το σύστημα ακολουθεί μια κλασική **3-Layer Client-Server** αρχιτεκτονική:
1. **Presentation Layer (Frontend)**: HTML5, CSS3 (Linear/Modern Cinematic Design) και JavaScript (Chart.js) που εκτελούνται εντός JavaFX WebView.
2. **Application Logic Layer (Backend)**: Java 17 με Maven και JDBC, το οποίο περιλαμβάνει:
   * **In-Memory Cache**: `ConcurrentHashMap` για 0ms καθυστέρηση σε συχνά ερωτήματα.
   * **SARGable Query Design**: Υποστήριξη σελιδοποίησης (Pagination) και dynamic sorting/filtering στη βάση.
3. **Database Layer (DBMS)**: MySQL 8.x Server με **InnoDB** engine, B-Tree Indexes, Views και Stored Procedures για Push-Down Query Processing.

---

## 🛠️ Οδηγίες Στήσιμου & Ρύθμισης (Setup Instructions)

### 1. Ρύθμιση της Βάσης Δεδομένων (MySQL Setup)
1. Εκκινήστε τον MySQL Server σας.
2. Δημιουργήστε τον χρήστη της εφαρμογής και δώστε του τα απαραίτητα δικαιώματα εκτελώντας το ακόλουθο script:
   ```sql
   CREATE USER 'mye030'@'localhost' IDENTIFIED BY 'mye030';
   GRANT ALL PRIVILEGES ON *.* TO 'mye030'@'localhost' WITH GRANT OPTION;
   FLUSH PRIVILEGES;
   ```
3. Εκτελέστε τα SQL scripts της βάσης με την εξής **υποχρεωτική σειρά**:
   * **`DDL/Schema.sql`**: Δημιουργία των πινάκων παραγωγής, constraints και B-Tree ευρετηρίων.
   * **`DDL/staging.sql`**: Δημιουργία staging πινάκων για την προσωρινή υποδοχή δεδομένων (Extract).
   * **`DDL/load_data.sql`**: Εντολές `LOAD DATA LOCAL INFILE` για τη φόρτωση των αρχικών CSV/XML δεδομένων.
   * **`DDL/etl.sql`**: Διαδικασία Transform & Load (String splitting συγγραφέων με Recursive CTE, Venue/Journal matching με Acronyms & Prefix-8 Join).
   * **`DDL/views.sql`**: Δημιουργία των SQL Views.

---

### 2. Εισαγωγή του Project στο Eclipse IDE
1. Ανοίξτε το **Eclipse IDE** (έκδοση 2023-09 ή νεότερη με υποστήριξη Java 17).
2. Επιλέξτε `File` -> `Import...` -> `Maven` -> `Existing Maven Projects`.
3. Επιλέξτε τον κατάλογο `DBMS` του project και πατήστε `Finish`.
4. Βεβαιωθείτε ότι το Project χρησιμοποιεί **JavaSE-17** και οι εξαρτήσεις Maven έχουν φορτωθεί σωστά (περιλαμβάνονται τα JavaFX 21, MySQL Connector/J 8.0.33, Jackson Databind, και JUnit 5).
5. (Προαιρετικό) Αν χρησιμοποιείτε Eclipse, σιγουρευτείτε ότι το πρόσθετο m2e έχει συσχετίσει σωστά το buildpath.

---

## 🚀 Εκτέλεση της Εφαρμογής (Running the Application)

### Από το Τερματικό (CLI)
Μεταβείτε στον φάκελο `DBMS` και εκτελέστε την παρακάτω εντολή:
```powershell
mvn clean compile javafx:run
```

### Από το Eclipse IDE
1. Κάντε δεξί κλικ στο project `dbms`.
2. Επιλέξτε `Run As` -> `Maven build...`.
3. Στο πεδίο **Goals** πληκτρολογήστε `javafx:run` και πατήστε `Run`.

---

## 🧪 Εκτέλεση των Δοκιμών (Running the Tests)
Το project διαθέτει μια εκτενή σουίτα δοκιμών με **31 αυτοματοποιημένα σενάρια ελέγχου** (JUnit 5) που καλύπτουν:
* Ανάκτηση προφίλ συγγραφέων και venues.
* Σωστή λειτουργία σελιδοποίησης (Pagination) και φιλτραρίσματος.
* Έλεγχο SQL Injection (ασφάλεια παραμετροποιημένων queries).
* Stress testing με 30 concurrent threads.

### Από το Τερματικό (CLI)
Μεταβείτε στον κατάλογο `DBMS` και εκτελέστε:
```powershell
mvn test
```

### Από το Eclipse IDE
1. Αναπτύξτε τον φάκελο `src/test/java`.
2. Κάντε δεξί κλικ στο πακέτο `dbms` (ή σε συγκεκριμένο αρχείο, π.χ. `DataServiceTest.java`).
3. Επιλέξτε `Run As` -> `JUnit Test`.

---

## 📋 Παραδοτέα (Deliverables Checklist)
Σύμφωνα με τις απαιτήσεις του μαθήματος, τα παρακάτω αρχεία είναι διαθέσιμα στην καθορισμένη δομή:
- [x] **Κώδικας & Δεδομένα**: Στους καταλόγους `src/` και `data/`.
- [x] **Τελική Αναφορά**: Διαθέσιμη ως [deliverables/5109_4977_report.pdf](deliverables/5109_4977_report.pdf).
- [x] **Βίντεο Παρουσίασης**: Διαθέσιμο ως [deliverables/πθτεβδ_final.mp4](deliverables/%CF%80%CE%B8%CF%84%CE%B5%CE%B2%CE%B4_final.mp4) (και ενσωματωμένο παραπάνω).
