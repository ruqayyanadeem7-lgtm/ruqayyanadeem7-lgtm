import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class Main {
    static final Color NAVY = new Color(0x1F2D3D), NAVY_LIGHT = new Color(0x2C3E50),
            ORANGE = new Color(0xE67E22), GREEN = new Color(0x27AE60), RED = new Color(0xC0392B),
            GREY = new Color(0x7F8C8D), BLUE = new Color(0x2980B9), PURPLE = new Color(0x8E44AD),
            TEAL = new Color(0x16A085), BACKGROUND = new Color(0xF4F6F8);
    /** Categories offered in the Type drop-down (the user may also type a new one). */
    static final String[] PET_TYPES = {"Dog", "Cat", "Bird", "Fish", "Rabbit", "Hamster", "Reptile",
            "Food", "Accessories", "Toys", "Medicine"};
    /** Health conditions that can be assigned to an animal. */
    static final String[] HEALTH_OPTIONS = {"Healthy", "Recovered", "Under Treatment", "Infected", "Quarantine"};
    /** Program entry point: applies the Nimbus theme and opens the login window. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception e) {
                System.out.println("Nimbus theme not applied: " + e.getMessage());
            }
            new LoginFrame().setVisible(true);
        });
    }
    /** Draws the round logo (orange ring, dark disc, white paw). */
    static void drawLogo(Graphics2D g2, int size) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(ORANGE);
        g2.fillOval(0, 0, size - 1, size - 1);
        g2.setColor(NAVY_LIGHT);
        int ring = Math.max(1, size / 12);
        g2.fillOval(ring, ring, size - 2 * ring - 1, size - 2 * ring - 1);
        g2.setColor(Color.WHITE);
        drawPaw(g2, 0, 0, size);
    }
    /** Draws a paw print (one big pad and four toes) in the current colour. */
    static void drawPaw(Graphics2D g2, int ox, int oy, int size) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.fillOval(ox + (int) (size * 0.29), oy + (int) (size * 0.50), (int) (size * 0.42), (int) (size * 0.30));
        int w = (int) (size * 0.15), h = (int) (size * 0.19);
        g2.fillOval(ox + (int) (size * 0.19), oy + (int) (size * 0.38), w, h);
        g2.fillOval(ox + (int) (size * 0.34), oy + (int) (size * 0.24), w, h);
        g2.fillOval(ox + (int) (size * 0.51), oy + (int) (size * 0.24), w, h);
        g2.fillOval(ox + (int) (size * 0.66), oy + (int) (size * 0.38), w, h);
    }
    /** Sets the logo as the icon of a window. */
    static void applyIcon(Window window) {
        List<Image> icons = new ArrayList<>();
        for (int s : new int[]{16, 32, 64}) {
            BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            drawLogo(g2, s);
            g2.dispose();
            icons.add(img);
        }
        window.setIconImages(icons);
    }
    /** Returns the colour used to display a health or vaccination status word. */
    static Color statusColor(String status) {
        switch (status) {
            case "Infected": case "Overdue": return RED;
            case "Under Treatment": case "Due Soon": return new Color(0xD35400);
            case "Quarantine": return PURPLE;
            case "Healthy": case "Recovered": case "Up to Date": return new Color(0x1E8449);
            case "Not Vaccinated": return new Color(0xB9770E);
            default: return GREY; // N/A, Completed
        }
    }
    /** Removes commas (CSV separator) and extra spaces from user text. */
    static String clean(String text) {
        return text == null ? "" : text.trim().replace(",", " ");
    }
    /** Gives a JTable the shared look: dark header, tall rows, zebra rows, no grid lines. */
    static void styleTable(JTable table, BiFunction<Integer, Object, Color> colorRule) {
        table.setRowHeight(34);
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(new Color(0xFAD7A0));
        table.setSelectionForeground(Color.BLACK);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));
        table.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                l.setBackground(NAVY);
                l.setForeground(Color.WHITE);
                l.setFont(new Font("SansSerif", Font.BOLD, 13));
                l.setBorder(new EmptyBorder(0, 10, 0, 10));
                return l;
            }
        });
        table.setDefaultRenderer(Object.class, new ZebraRenderer(colorRule));
    }
    /** Custom checked exception: thrown for invalid input, forbidden actions and file problems. */
    static class PetShopException extends Exception {
        private static final long serialVersionUID = 1L;
        PetShopException(String message) { super(message); }
        PetShopException(String message, Throwable cause) { super(message, cause); }
    }
    /** Role - the user types; each role decides which actions the user may perform. */
    enum Role {
        ADMIN("Admin"), VETERINARIAN("Veterinarian"), STAFF("Staff");
        private final String label;
        Role(String label) { this.label = label; }
        String getLabel() { return label; }
        boolean canManageStock() { return this == ADMIN || this == STAFF; }      // add, update, sell, adopt
        boolean canDelete() { return this == ADMIN; }                            // delete pets
        boolean canEditHealth() { return this == ADMIN || this == VETERINARIAN; } // health records
        boolean canViewSales() { return this == ADMIN || this == STAFF; }        // histories, customers
        boolean canManageUsers() { return this == ADMIN; }                       // users, sample data
        @Override
        public String toString() { return label; }
    }
    /** User - a person who can log in. Only the SHA-256 hash of the password is stored. */
    static class User {
        private final String username;
        private final String passwordHash;
        private final Role role;
        User(String username, String passwordHash, Role role) {
            this.username = username;
            this.passwordHash = passwordHash;
            this.role = role;
        }
        String getUsername() { return username; }
        String getPasswordHash() { return passwordHash; }
        Role getRole() { return role; }
        /** Converts the password into a SHA-256 hash written in hexadecimal. */
        static String hash(String password) {
            try {
                byte[] digest = MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (byte b : digest) sb.append(String.format("%02x", b));
                return sb.toString();
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("SHA-256 is not available on this computer", e);
            }
        }
        /** Checks a new username/password pair. */
        static void validateNewAccount(String username, String password, List<User> existing) throws PetShopException {
            if (!username.matches("[A-Za-z0-9_]{3,20}")) {
                throw new PetShopException("Username must be 3-20 characters (letters, numbers or underscore).");
            }
            if (password.length() < 4) throw new PetShopException("Password must be at least 4 characters long.");
            for (User u : existing) {
                if (u.getUsername().equalsIgnoreCase(username)) throw new PetShopException("This username is already taken.");
            }
        }
        String toCsv() { return username + "," + passwordHash + "," + role.name(); }
        static User fromCsv(String line) throws PetShopException {
            try {
                String[] p = line.split(",", -1);
                return new User(p[0].trim(), p[1].trim(), Role.valueOf(p[2].trim()));
            } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
                throw new PetShopException("Corrupted record in users file: " + line, e);
            }
        }
    }
    /** Pet - one pet or shop item. Animals have health and vaccinations; items have health "N/A". */
    static class Pet {
        private static final List<String> ITEM_TYPES = Arrays.asList("food", "accessories", "toys", "medicine");
        private final int id;
        private String name, type, health, photo;
        private double price;
        private int quantity;
        Pet(int id, String name, String type, double price, int quantity, String health, String photo) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.price = price;
            this.quantity = quantity;
            this.health = health;
            this.photo = photo;
        }
        int getId() { return id; }
        String getName() { return name; }
        String getType() { return type; }
        double getPrice() { return price; }
        int getQuantity() { return quantity; }
        String getHealth() { return health; }
        String getPhoto() { return photo; }
        void setName(String name) { this.name = name; }
        void setType(String type) { this.type = type; }
        void setPrice(double price) { this.price = price; }
        void setQuantity(int quantity) { this.quantity = quantity; }
        void setHealth(String health) { this.health = health; }
        void setPhoto(String photo) { this.photo = photo; }
        /** True when the type is a living animal (not food, accessories, toys or medicine). */
        static boolean isAnimalType(String type) { return !ITEM_TYPES.contains(type.trim().toLowerCase()); }
        boolean isAnimal() { return isAnimalType(type); }
        /** Fish, hamsters and reptiles have no vaccines in this system. */
        boolean needsVaccination() {
            return isAnimal() && !Arrays.asList("fish", "hamster", "reptile").contains(type.toLowerCase());
        }
        /** "N/A" for items, "Healthy" when nothing is set for an animal. */
        static String defaultHealth(String type, String current) {
            if (!isAnimalType(type)) return "N/A";
            return (current == null || current.isEmpty() || current.equals("N/A")) ? "Healthy" : current;
        }
        String toCsv() { return id + "," + name + "," + type + "," + price + "," + quantity + "," + health + "," + photo; }
        /** Builds a Pet from one CSV line. */
        static Pet fromCsv(String line) throws PetShopException {
            try {
                String[] p = line.split(",", -1);
                if (p.length < 5) throw new PetShopException("Corrupted record in pets file: " + line);
                String type = p[2].trim();
                return new Pet(Integer.parseInt(p[0].trim()), p[1].trim(), type, Double.parseDouble(p[3].trim()),
                        Integer.parseInt(p[4].trim()), defaultHealth(type, p.length > 5 ? p[5].trim() : ""),
                        p.length > 6 ? p[6].trim() : "");
            } catch (NumberFormatException e) {
                throw new PetShopException("Corrupted record in pets file: " + line, e);
            }
        }
    }
    /** Vaccination - one vaccine given to one pet, plus the logic that works out a pet's vaccination status. */
    static class Vaccination {
        /** A vaccine is "Due Soon" when its next dose is within this many days. */
        static final int SOON_DAYS = 30;
        private final int petId;
        private final String vaccine, givenBy;
        private final LocalDate given, nextDue;
        Vaccination(int petId, String vaccine, LocalDate given, LocalDate nextDue, String givenBy) {
            this.petId = petId;
            this.vaccine = vaccine;
            this.given = given;
            this.nextDue = nextDue;
            this.givenBy = givenBy;
        }
        int getPetId() { return petId; }
        String getVaccine() { return vaccine; }
        LocalDate getGiven() { return given; }
        LocalDate getNextDue() { return nextDue; }
        String getGivenBy() { return givenBy; }
        /** Status of a single due date compared with today. */
        static String statusOf(LocalDate nextDue) {
            LocalDate today = LocalDate.now();
            if (nextDue.isBefore(today)) return "Overdue";
            return nextDue.isAfter(today.plusDays(SOON_DAYS)) ? "Up to Date" : "Due Soon";
        }
        /** Returns only the records that belong to one pet. */
        static List<Vaccination> forPet(List<Vaccination> all, int petId) {
            List<Vaccination> result = new ArrayList<>();
            for (Vaccination v : all) if (v.petId == petId) result.add(v);
            return result;
        }
        /** For each vaccine name, keeps only the most recent dose. */
        static Map<String, Vaccination> latestByVaccine(List<Vaccination> records) {
            Map<String, Vaccination> latest = new LinkedHashMap<>();
            for (Vaccination v : records) {
                Vaccination old = latest.get(v.vaccine.toLowerCase());
                if (old == null || v.given.isAfter(old.given)) latest.put(v.vaccine.toLowerCase(), v);
            }
            return latest;
        }
        /** Overall vaccination status of a pet, based on its most recent doses. */
        static String overallStatus(Pet pet, List<Vaccination> all) {
            if (!pet.needsVaccination()) return "N/A";
            List<Vaccination> mine = forPet(all, pet.getId());
            if (mine.isEmpty()) return "Not Vaccinated";
            String result = "Up to Date";
            for (Vaccination v : latestByVaccine(mine).values()) {
                String s = statusOf(v.nextDue);
                if (s.equals("Overdue")) return "Overdue";
                if (s.equals("Due Soon")) result = "Due Soon";
            }
            return result;
        }
        String toCsv() { return petId + "," + vaccine + "," + given + "," + nextDue + "," + givenBy; }
        static Vaccination fromCsv(String line) throws PetShopException {
            try {
                String[] p = line.split(",", -1);
                return new Vaccination(Integer.parseInt(p[0].trim()), p[1].trim(),
                        LocalDate.parse(p[2].trim()), LocalDate.parse(p[3].trim()), p[4].trim());
            } catch (NumberFormatException | DateTimeParseException | ArrayIndexOutOfBoundsException e) {
                throw new PetShopException("Corrupted record in vaccinations file: " + line, e);
            }
        }
    }
    /** FileManager - handles ALL file input and output. Everything lives in the "data" folder. */
    static class FileManager {
        private static final Path DATA = Paths.get("data");
        private static final Path IMAGES = DATA.resolve("images");
        private static final Path PETS = DATA.resolve("pets.csv");
        private static final Path SALES = DATA.resolve("sales.csv");
        private static final Path ADOPTIONS = DATA.resolve("adoptions.csv");
        private static final Path VACCINES = DATA.resolve("vaccinations.csv");
        private static final Path USERS = DATA.resolve("users.csv");
        private static String now() {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        /** Reads all non-empty lines of a file (UTF-8); creates the folders and the file when missing. */
        private static List<String> read(Path file) throws PetShopException {
            try {
                Files.createDirectories(IMAGES);
                if (!Files.exists(file)) Files.createFile(file);
                List<String> lines = new ArrayList<>();
                for (String l : Files.readAllLines(file, StandardCharsets.UTF_8)) if (!l.trim().isEmpty()) lines.add(l);
                return lines;
            } catch (IOException e) {
                throw new PetShopException("Could not read the file " + file.getFileName(), e);
            }
        }
        /** Writes lines to a file (UTF-8), either replacing or appending. */
        private static void write(Path file, List<String> lines, boolean append) throws PetShopException {
            try {
                Files.createDirectories(IMAGES);
                Files.write(file, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                        append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                throw new PetShopException("Could not save the file " + file.getFileName(), e);
            }
        }
        /** Saves an urgent animal case without changing existing pet/shop records. */
        static void recordEmergency(String animal, String type, String owner, String phone,
                                    String urgency, String symptoms, String notes, String staff)
                throws PetShopException {
            Path file = DATA.resolve("emergency_cases.csv");
            String[] values = {now(), animal, type, owner, phone, urgency, symptoms, notes, staff};
            List<String> row = new ArrayList<>();
            for (String value : values) {
                String safe = value == null ? "" : value.replace("\"", "\"\"");
                row.add("\"" + safe.replace("\r", " ").replace("\n", " ") + "\"");
            }
            write(file, Collections.singletonList(String.join(",", row)), true);
        }
        /** Loads saved emergency cases for the Emergency Cases dashboard button. */
        static List<String[]> loadEmergencyCases() throws PetShopException {
            List<String[]> cases = new ArrayList<>();
            for (String line : read(DATA.resolve("emergency_cases.csv"))) {
                List<String> fields = new ArrayList<>();
                StringBuilder value = new StringBuilder();
                boolean quoted = false;
                for (int i = 0; i < line.length(); i++) {
                    char ch = line.charAt(i);
                    if (ch == '\"') {
                        if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                            value.append('\"'); i++;
                        } else quoted = !quoted;
                    } else if (ch == ',' && !quoted) {
                        fields.add(value.toString()); value.setLength(0);
                    } else value.append(ch);
                }
                fields.add(value.toString());
                while (fields.size() < 9) fields.add("");
                cases.add(new String[]{fields.get(0), fields.get(1), fields.get(2), fields.get(3),
                        fields.get(4), fields.get(5), fields.get(6), fields.get(7), fields.get(8)});
            }
            Collections.reverse(cases); // newest cases appear first
            return cases;
        }
        static void saveManagementRecord(String fileName, String[] values) throws PetShopException {
            Path file = DATA.resolve(fileName);
            List<String> row = new ArrayList<>();
            for (String value : values) {
                String safe = value == null ? "" : value.replace("\"", "\"\"")
                        .replace("\r", " ").replace("\n", " ");
                row.add("\"" + safe + "\"");
            }
            write(file, Collections.singletonList(String.join(",", row)), true);
        }
        static List<String[]> loadManagementRecords(String fileName, int columns) throws PetShopException {
            List<String[]> records = new ArrayList<>();
            for (String line : read(DATA.resolve(fileName))) {
                List<String> fields = new ArrayList<>();
                StringBuilder value = new StringBuilder(); boolean quoted = false;
                for (int i = 0; i < line.length(); i++) {
                    char ch = line.charAt(i);
                    if (ch == '\"') {
                        if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '\"') { value.append('\"'); i++; }
                        else quoted = !quoted;
                    } else if (ch == ',' && !quoted) { fields.add(value.toString()); value.setLength(0); }
                    else value.append(ch);
                }
                fields.add(value.toString());
                while (fields.size() < columns) fields.add("");
                records.add(fields.subList(0, columns).toArray(new String[0]));
            }
            Collections.reverse(records);
            return records;
        }
        static List<Pet> loadPets() throws PetShopException {
            List<Pet> list = new ArrayList<>();
            for (String line : read(PETS)) list.add(Pet.fromCsv(line));
            return list;
        }
        static void savePets(List<Pet> pets) throws PetShopException {
            List<String> lines = new ArrayList<>();
            for (Pet p : pets) lines.add(p.toCsv());
            write(PETS, lines, false);
        }
        static List<Vaccination> loadVaccinations() throws PetShopException {
            List<Vaccination> list = new ArrayList<>();
            for (String line : read(VACCINES)) list.add(Vaccination.fromCsv(line));
            return list;
        }
        static void saveVaccinations(List<Vaccination> records) throws PetShopException {
            List<String> lines = new ArrayList<>();
            for (Vaccination v : records) lines.add(v.toCsv());
            write(VACCINES, lines, false);
        }
        /** Reads all users. When the file is empty the three default accounts are created. */
        static List<User> loadUsers() throws PetShopException {
            List<User> list = new ArrayList<>();
            for (String line : read(USERS)) list.add(User.fromCsv(line));
            if (list.isEmpty()) {
                list.add(new User("admin", User.hash("1234"), Role.ADMIN));
                list.add(new User("vet", User.hash("vet123"), Role.VETERINARIAN));
                list.add(new User("staff", User.hash("staff123"), Role.STAFF));
                saveUsers(list);
            }
            return list;
        }
        static void saveUsers(List<User> users) throws PetShopException {
            List<String> lines = new ArrayList<>();
            for (User u : users) lines.add(u.toCsv());
            write(USERS, lines, false);
        }
        /** Appends one sale: date, item, quantity, unit price, total, seller, customer, phone, address. */
        static void recordSale(Pet p, int qty, String soldBy, String customer, String phone, String address)
                throws PetShopException {
            write(SALES, Collections.singletonList(now() + "," + p.getName() + "," + qty + "," + p.getPrice() + ","
                    + (qty * p.getPrice()) + "," + soldBy + "," + customer + "," + phone + "," + address), true);
        }
        /** Reads every sale as 9 values (old records without customer details show "-"). */
        static List<String[]> loadSales() throws PetShopException {
            List<String[]> sales = new ArrayList<>();
            try {
                for (String line : read(SALES)) {
                    String[] parts = line.split(",", -1);
                    if (parts.length >= 5) {
                        Double.parseDouble(parts[4]); // validates the record
                        String[] row = Arrays.copyOf(parts, 9);
                        for (int i = 5; i < 9; i++) if (row[i] == null || row[i].trim().isEmpty()) row[i] = "-";
                        sales.add(row);
                    }
                }
            } catch (NumberFormatException e) {
                throw new PetShopException("The sales file contains a corrupted record.", e);
            }
            return sales;
        }
        /** Returns the total revenue earned so far. */
        static double getTotalSales() throws PetShopException {
            double total = 0;
            for (String[] row : loadSales()) total += Double.parseDouble(row[4]);
            return total;
        }
        /** Appends one adoption: date, pet, type, adopter, phone, address, fee, handled by. */
        static void recordAdoption(Pet p, String adopter, String phone, String address, double fee, String by)
                throws PetShopException {
            write(ADOPTIONS, Collections.singletonList(now() + "," + p.getName() + "," + p.getType() + ","
                    + adopter + "," + phone + "," + address + "," + fee + "," + by), true);
        }
        /** Reads every adoption as 8 values. */
        static List<String[]> loadAdoptions() throws PetShopException {
            List<String[]> list = new ArrayList<>();
            try {
                for (String line : read(ADOPTIONS)) {
                    String[] parts = line.split(",", -1);
                    if (parts.length >= 8) {
                        Double.parseDouble(parts[6]); // validates the fee
                        list.add(parts);
                    }
                }
            } catch (NumberFormatException e) {
                throw new PetShopException("The adoptions file contains a corrupted record.", e);
            }
            return list;
        }
        /** Copies a photo chosen by the user into the images folder and returns the new file name. */
        static String savePhoto(File source, int petId) throws PetShopException {
            String name = source.getName().toLowerCase();
            String ext = name.substring(name.lastIndexOf('.') + 1);
            if (!Arrays.asList("jpg", "jpeg", "jfif", "png", "gif", "bmp").contains(ext)) {
                throw new PetShopException("Please choose a JPG, PNG, GIF or BMP image.");
            }
            try {
                if (ImageIO.read(source) == null) throw new PetShopException("The selected file is not a valid image.");
                Files.createDirectories(IMAGES);
                String fileName = petId + "_" + System.currentTimeMillis() + "." + ext;
                Files.copy(source.toPath(), IMAGES.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                return fileName;
            } catch (IOException e) {
                throw new PetShopException("Could not save the photo.", e);
            }
        }
        /** Loads a stored photo; returns null when there is none or it cannot be read. */
        static BufferedImage loadPhoto(String fileName) {
            if (fileName == null || fileName.isEmpty()) return null;
            try {
                return ImageIO.read(IMAGES.resolve(fileName).toFile());
            } catch (IOException e) {
                return null;
            }
        }
        /** Deletes a stored photo (a missing file is ignored). */
        static void deletePhoto(String fileName) {
            if (fileName != null && !fileName.isEmpty()) IMAGES.resolve(fileName).toFile().delete();
        }
    }
    /** Loads category photos from direct image URLs, then tries the local images folder. */
    static class CategoryPhotos {
        private static final Map<String, BufferedImage> CACHE = new HashMap<>();
        private static final Map<String, String> URLS = new HashMap<>();
        static {
            URLS.put("dog", "https://images.unsplash.com/photo-1552053831-71594a27632d?auto=format&fit=crop&w=800&q=85");
            URLS.put("cat", "https://static.vecteezy.com/system/resources/thumbnails/068/222/926/small/close-up-of-a-charming-ginger-kitten-with-big-blue-eyes-resting-on-a-soft-blanket-photo.jpg");
            URLS.put("rabbit", "https://a-z-animals.com/media/2026/08/shutterstock-1088849552-huge-licensed-scaled.jpg");
            URLS.put("bird", "https://www.birdlife.org/wp-content/uploads/2021/06/Puffins-Wynand-van-Poortvliet-Unsplash-edited-scaled.jpg");
            URLS.put("fish", "https://images.unsplash.com/photo-1524704654690-b56c05c78a00?auto=format&fit=crop&w=800&h=600&q=85");
            URLS.put("hamster", "https://cdn.pixabay.com/photo/2026/04/26/10/50/erwinbosman-hamster-10247468_640.jpg");
            URLS.put("food", "https://images.unsplash.com/photo-1589924691995-400dc9ecc119?auto=format&fit=crop&w=800&h=600&q=85");
            URLS.put("accessories", "https://petspark.pk/cdn/shop/collections/71qV3F_828L._AC_SL1000_1200x1200.jpg?v=1692034113");
            URLS.put("toys", "https://images.unsplash.com/photo-1559454403-b8fb88521f11?auto=format&fit=crop&w=800&q=85");
            URLS.put("medicine", "https://images.unsplash.com/photo-1585435557343-3b092031a831?auto=format&fit=crop&w=800&q=85");
            URLS.put("reptile", "https://images.unsplash.com/photo-1535591273668-578e31182c4f?auto=format&fit=crop&w=800&q=80");
        }
        /** Maps category/type text to its image key. */
        static String keyFor(String type) {
            if (type == null) return null;
            switch (type.trim().toLowerCase()) {
                case "dog": case "dogs": return "dog";
                case "cat": case "cats": return "cat";
                case "rabbit": case "rabbits": return "rabbit";
                case "bird": case "birds": return "bird";
                case "fish": return "fish";
                case "hamster": case "hamsters": return "hamster";
                case "reptile": case "reptiles": return "reptile";
                case "food": case "pet food": return "food";
                case "accessories": return "accessories";
                case "toy": case "toys": return "toys";
                case "medicine": case "medicines": return "medicine";
                default: return null;
            }
        }
        /** Returns a cached image; if online loading fails, tries images/<key>.jpg. */
        static BufferedImage get(String type) {
            String key = keyFor(type);
            if (key == null) return null;
            if (CACHE.containsKey(key)) return CACHE.get(key);
            BufferedImage img = null;
            String imageUrl = URLS.get(key);
            if (imageUrl != null) {
                try {
                    URLConnection connection = new URL(imageUrl).openConnection();
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(7000);
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                    try (java.io.InputStream input = connection.getInputStream()) {
                        img = ImageIO.read(input);
                    }
                } catch (Exception e) {
                    System.out.println("Online category image could not load for " + key + ": " + e.getMessage());
                }
            }
            if (img == null) {
                for (String dir : new String[]{"images", "src/images", "../images"}) {
                    File f = new File(dir, key + ".jpg");
                    if (f.exists()) {
                        try {
                            img = ImageIO.read(f);
                        } catch (IOException e) {
                            System.out.println("Could not read image " + f.getAbsolutePath() + ": " + e.getMessage());
                        }
                        if (img != null) break;
                    }
                }
            }
            CACHE.put(key, img);
            return img;
        }
    }
    /** ReadOnlyModel - a table model whose cells cannot be edited (used by every table). */
    static class ReadOnlyModel extends DefaultTableModel {
        private static final long serialVersionUID = 1L;
        ReadOnlyModel(String[] columns) { super(columns, 0); }
        @Override
        public boolean isCellEditable(int row, int col) { return false; }
    }
    /** LogoPanel - draws the Snooby Wobby paw logo with Java 2D. */
    static class LogoPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final int size;
        LogoPanel(int size) {
            this.size = size;
            setOpaque(false);
            setPreferredSize(new Dimension(size, size));
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            drawLogo(g2, size);
            g2.dispose();
        }
    }
    /** PhotoPanel - shows a picture scaled to fit, or a grey paw and "No photo". */
    static class PhotoPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private transient BufferedImage image;
        PhotoPanel(int width, int height) {
            setPreferredSize(new Dimension(width, height));
            setOpaque(false);
        }
        /** Sets the picture to show (null shows the placeholder). */
        void setImage(BufferedImage image) {
            this.image = image;
            repaint();
        }
        /** Shows the picture of a category type (Dog, Cat, Food...). */
        void showCategory(String type) { setImage(CategoryPhotos.get(type)); }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(0xEEF1F4));
            g2.fillRoundRect(0, 0, w - 1, h - 1, 14, 14);
            if (image != null) {
                double scale = Math.min((double) (w - 8) / image.getWidth(), (double) (h - 8) / image.getHeight());
                int iw = (int) (image.getWidth() * scale), ih = (int) (image.getHeight() * scale);
                g2.drawImage(image, (w - iw) / 2, (h - ih) / 2, iw, ih, null);
            } else {
                g2.setColor(new Color(0xB8C0C8));
                int paw = Math.min(w, h) - 40;
                drawPaw(g2, (w - paw) / 2, 4, paw);
                g2.setFont(new Font("SansSerif", Font.ITALIC, 12));
                g2.drawString("No photo", (w - g2.getFontMetrics().stringWidth("No photo")) / 2, h - 10);
            }
            g2.setColor(new Color(0xD5DADF));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
            g2.dispose();
        }
    }
    /** StyledButton - a flat, rounded, coloured button that looks the same on every theme. */
    static class StyledButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final Color base;
        private boolean restricted; // true when the user's role may not use this button
        StyledButton(String text, Color base) {
            super(text);
            this.base = base;
            setForeground(Color.WHITE);
            setFont(new Font("SansSerif", Font.BOLD, 13));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(8, 10, 8, 10));
        }
        /** Marks the button as not allowed for the current role (it is painted pale). */
        void setRestricted(boolean restricted) {
            this.restricted = restricted;
            repaint();
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor((!isEnabled() || restricted) ? new Color(0xC5CBD1)
                    : (getModel().isPressed() ? base.darker() : base));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }
    /** ZebraRenderer - alternate row colours; a colour rule can make chosen cells bold and coloured. */
    static class ZebraRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private final transient BiFunction<Integer, Object, Color> colorRule;
        ZebraRenderer(BiFunction<Integer, Object, Color> colorRule) { this.colorRule = colorRule; }
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, r, c);
            l.setBorder(new EmptyBorder(0, 10, 0, 10));
            if (!sel) l.setBackground(r % 2 == 0 ? Color.WHITE : new Color(0xF8F9FA));
            Color special = colorRule == null ? null : colorRule.apply(c, v);
            l.setForeground(special != null ? special : Color.BLACK);
            l.setFont(l.getFont().deriveFont(special != null ? Font.BOLD : Font.PLAIN));
            return l;
        }
    }
    /** LoginFrame - first window. Checks the credentials and opens the dashboard for that role. */
    static class LoginFrame extends JFrame {
        private static final long serialVersionUID = 1L;
        private final JTextField txtUser = new JTextField(16);
        private final JPasswordField txtPass = new JPasswordField(16);
        LoginFrame() {
            setTitle("Snooby Wobby - Login");
            applyIcon(this);
            setSize(440, 640);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setLocationRelativeTo(null);
            setResizable(false);
            JPanel root = new JPanel(new GridBagLayout());
            root.setBackground(NAVY);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            JPanel logoHolder = new JPanel(new FlowLayout(FlowLayout.CENTER));
            logoHolder.setOpaque(false);
            logoHolder.add(new LogoPanel(110));
            c.gridy = 0; c.insets = new Insets(10, 40, 5, 40);
            root.add(logoHolder, c);
            JLabel title = new JLabel("Snooby Wobby", SwingConstants.CENTER);
            title.setFont(new Font("Serif", Font.BOLD, 32));
            title.setForeground(Color.WHITE);
            c.gridy = 1; c.insets = new Insets(5, 40, 0, 40);
            root.add(title, c);
            JLabel sub = new JLabel("Pet Shop & Clinic Management System", SwingConstants.CENTER);
            sub.setForeground(new Color(0xBDC3C7));
            c.gridy = 2; c.insets = new Insets(0, 40, 20, 40);
            root.add(sub, c);
            c.insets = new Insets(4, 40, 4, 40);
            c.gridy = 3; root.add(makeLabel("Username"), c);
            c.gridy = 4; root.add(txtUser, c);
            c.gridy = 5; root.add(makeLabel("Password"), c);
            c.gridy = 6; root.add(txtPass, c);
            JCheckBox chkShow = new JCheckBox("Show password");
            chkShow.setOpaque(false);
            chkShow.setForeground(new Color(0xBDC3C7));
            chkShow.addActionListener(e -> txtPass.setEchoChar(chkShow.isSelected() ? (char) 0 : '\u2022'));
            c.gridy = 7; root.add(chkShow, c);
            StyledButton btnLogin = new StyledButton("LOGIN", ORANGE);
            c.gridy = 8; c.insets = new Insets(14, 40, 6, 40);
            root.add(btnLogin, c);
            JButton btnRegister = new JButton("New staff member? Create an account");
            btnRegister.setForeground(new Color(0xF5B041));
            btnRegister.setContentAreaFilled(false);
            btnRegister.setBorderPainted(false);
            btnRegister.setFocusPainted(false);
            btnRegister.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            c.gridy = 9; c.insets = new Insets(2, 40, 2, 40);
            root.add(btnRegister, c);
            JLabel hint = new JLabel("<html><center>Demo accounts<br>admin / 1234 &nbsp;&bull;&nbsp; vet / vet123"
                    + " &nbsp;&bull;&nbsp; staff / staff123</center></html>", SwingConstants.CENTER);
            hint.setForeground(new Color(0x95A5A6));
            hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
            c.gridy = 10; c.insets = new Insets(12, 20, 5, 20);
            root.add(hint, c);
            btnLogin.addActionListener(e -> attemptLogin());
            txtPass.addActionListener(e -> attemptLogin()); // Enter key also logs in
            btnRegister.addActionListener(e -> openRegisterDialog());
            setContentPane(root);
        }
        private JLabel makeLabel(String text) {
            JLabel l = new JLabel(text);
            l.setForeground(Color.WHITE);
            l.setFont(new Font("SansSerif", Font.BOLD, 12));
            return l;
        }
        /** Checks the credentials, then opens the dashboard or shows an error. */
        private void attemptLogin() {
            String name = txtUser.getText().trim();
            String hash = User.hash(new String(txtPass.getPassword()));
            try {
                for (User u : FileManager.loadUsers()) {
                    if (u.getUsername().equalsIgnoreCase(name) && u.getPasswordHash().equals(hash)) {
                        new DashboardFrame(u).setVisible(true);
                        dispose();
                        return;
                    }
                }
                JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                txtPass.setText("");
            } catch (PetShopException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        /** Lets a new staff member create an account (the Staff role only). */
        private void openRegisterDialog() {
            JTextField user = new JTextField();
            JPasswordField pass1 = new JPasswordField(), pass2 = new JPasswordField();
            JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
            panel.add(new JLabel("Username (3-20 letters, numbers or _)"));
            panel.add(user);
            panel.add(new JLabel("Password (at least 4 characters)"));
            panel.add(pass1);
            panel.add(new JLabel("Confirm password"));
            panel.add(pass2);
            panel.add(new JLabel("Account type: Staff"));
            while (true) {
                if (JOptionPane.showConfirmDialog(this, panel, "Create Staff Account",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
                try {
                    String name = user.getText().trim().toLowerCase();
                    String password = new String(pass1.getPassword());
                    if (!password.equals(new String(pass2.getPassword()))) {
                        throw new PetShopException("The two passwords do not match.");
                    }
                    List<User> users = FileManager.loadUsers();
                    User.validateNewAccount(name, password, users);
                    users.add(new User(name, User.hash(password), Role.STAFF));
                    FileManager.saveUsers(users);
                    txtUser.setText(name);
                    txtPass.setText("");
                    JOptionPane.showMessageDialog(this, "Account created. You can log in now.");
                    return;
                } catch (PetShopException e) {
                    JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    /**
     * DashboardFrame - main window. Depending on the role it lets the user add,
     * update, delete, search, filter, sell and adopt out pets, attach photos and
     * open health records. Every change is saved to file immediately.
     */
    static class DashboardFrame extends JFrame {
        private static final long serialVersionUID = 1L;
        private static final int LOW_STOCK_LIMIT = 5;
        private static final int COL_STOCK = 4;
        /** Details collected from the buyer / adopter in the sell and adopt forms. */
        private static class Deal {
            int qty = 1;
            String customer, phone, address = "-";
            double fee;
        }
        private final transient User currentUser;
        private transient List<Pet> pets = new ArrayList<>();
        private transient List<Vaccination> vaccinations = new ArrayList<>();
        private File pendingPhoto; // photo chosen before the pet has been saved
        private float interfaceScale = 1.10f; // slightly larger default interface
        private final ReadOnlyModel model = new ReadOnlyModel(
                new String[]{"ID", "Pet / Item Name", "Type", "Price (Rs)", "Stock", "Health", "Vaccination"});
        private final JTable table = new JTable(model);
        private final JTextField txtName = new JTextField(), txtPrice = new JTextField(),
                txtQty = new JTextField(), txtSearch = new JTextField(12);
        private final JComboBox<String> cmbType = new JComboBox<>(PET_TYPES);
        private final JComboBox<String> cmbTypeFilter = new JComboBox<>(typeFilterItems());
        private final JComboBox<String> cmbStatusFilter = new JComboBox<>(new String[]{
                "All Status", "Healthy", "Recovered", "Under Treatment", "Infected", "Quarantine",
                "Up to Date", "Due Soon", "Overdue", "Not Vaccinated"});
        private final JLabel lblStatus = new JLabel(" ");
        private final PhotoPanel photoPanel = new PhotoPanel(240, 130);
        private final JLabel lblPets = new JLabel("0"), lblValue = new JLabel("0"), lblLow = new JLabel("0"),
                lblSales = new JLabel("0"), lblInfected = new JLabel("0"), lblVaccDue = new JLabel("0");
        private final StyledButton btnAdd = new StyledButton("Add Pet", ORANGE);
        private final StyledButton btnUpdate = new StyledButton("Update", NAVY_LIGHT);
        private final StyledButton btnDelete = new StyledButton("Delete", RED);
        private final StyledButton btnSell = new StyledButton("Sell", GREEN);
        private final StyledButton btnAdopt = new StyledButton("Adopt Pet", PURPLE);
        private final StyledButton btnHealth = new StyledButton("Health & Vaccination", TEAL);
        private final StyledButton btnEmergency = new StyledButton("EMERGENCY CASE", RED);
        private final StyledButton btnClear = new StyledButton("Clear Form", GREY);
        private final StyledButton btnPhoto = new StyledButton("Choose Photo", NAVY_LIGHT);
        private final StyledButton btnHistory = new StyledButton("Sales History", BLUE);
        private final StyledButton btnAdoptions = new StyledButton("Adoption History", PURPLE);
        private final StyledButton btnCustomers = new StyledButton("Customers", TEAL);
        private final StyledButton btnEmergencyHistory = new StyledButton("Emergency Cases", RED);
        private final StyledButton btnLogout = new StyledButton("Logout", RED);
        DashboardFrame(User user) {
            this.currentUser = user;
            setTitle("Snooby Wobby - Pet Shop & Clinic Management");
            applyIcon(this);
            setSize(1480, 840);
            setMinimumSize(new Dimension(1280, 740));
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setLocationRelativeTo(null);
            JPanel root = new JPanel(new BorderLayout());
            root.setBackground(BACKGROUND);
            root.add(buildHeader(), BorderLayout.NORTH);
            JPanel body = new JPanel(new BorderLayout(12, 0));
            body.setOpaque(false);
            body.setBorder(new EmptyBorder(12, 14, 14, 14));
            body.add(buildCenter(), BorderLayout.CENTER);
            body.add(buildFormPanel(), BorderLayout.EAST);
            root.add(body, BorderLayout.CENTER);
            setContentPane(root);
            setJMenuBar(buildMenuBar());
            installZoomShortcuts();
            applyInterfaceScale();
            applyPermissions();
            loadData();
        }
        /** Ctrl + Plus/Equals enlarges the interface; Ctrl + Minus shrinks it; Ctrl + 0 resets. */
        private void installZoomShortcuts() {
            JRootPane rp = getRootPane();
            InputMap im = rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = rp.getActionMap();
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK), "zoomIn");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK), "zoomIn");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK), "zoomIn");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), "zoomOut");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK), "zoomOut");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK), "zoomReset");
            am.put("zoomIn", new AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e) {
                interfaceScale = Math.min(1.50f, interfaceScale + 0.05f); applyInterfaceScale();
            }});
            am.put("zoomOut", new AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e) {
                interfaceScale = Math.max(0.85f, interfaceScale - 0.05f); applyInterfaceScale();
            }});
            am.put("zoomReset", new AbstractAction() { public void actionPerformed(java.awt.event.ActionEvent e) {
                interfaceScale = 1.10f; applyInterfaceScale();
            }});
        }
        private void applyInterfaceScale() {
            scaleComponentTree(getContentPane(), interfaceScale);
            table.setRowHeight(Math.round(34 * interfaceScale));
            table.getTableHeader().setPreferredSize(new Dimension(0, Math.round(36 * interfaceScale)));
            revalidate();
            repaint();
        }
        private void scaleComponentTree(Component component, float scale) {
            if (component instanceof JComponent) {
                JComponent jc = (JComponent) component;
                Font base = (Font) jc.getClientProperty("snooby.baseFont");
                if (base == null) {
                    base = jc.getFont();
                    if (base != null) jc.putClientProperty("snooby.baseFont", base);
                }
                if (base != null) jc.setFont(base.deriveFont(Math.max(9f, base.getSize2D() * scale)));
            }
            if (component instanceof Container) {
                for (Component child : ((Container) component).getComponents()) scaleComponentTree(child, scale);
            }
        }
        /** Items of the type filter: "All Types" followed by every category. */
        private static String[] typeFilterItems() {
            String[] items = new String[PET_TYPES.length + 1];
            items[0] = "All Types";
            System.arraycopy(PET_TYPES, 0, items, 1, PET_TYPES.length);
            return items;
        }
        private String askText(String prompt, String title) {
            return JOptionPane.showInputDialog(this, prompt, title, JOptionPane.QUESTION_MESSAGE);
        }
        private void manageAppointments() {
            String[] options = {"Add Appointment", "View Appointments", "Cancel"};
            int choice = JOptionPane.showOptionDialog(this, "Manage clinic appointments", "Appointment Management",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
            if (choice == 0) {
                String pet = askText("Pet name / ID:", "New Appointment"); if (pet == null) return;
                String owner = askText("Owner name:", "New Appointment"); if (owner == null) return;
                String phone = askText("Contact number:", "New Appointment"); if (phone == null) return;
                String date = askText("Appointment date (YYYY-MM-DD):", "New Appointment"); if (date == null) return;
                try { LocalDate.parse(date.trim()); } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Please enter the date as YYYY-MM-DD.", "Invalid Date", JOptionPane.WARNING_MESSAGE); return; }
                String time = askText("Appointment time (e.g. 10:30 AM):", "New Appointment"); if (time == null) return;
                String vet = askText("Veterinarian / staff:", "New Appointment"); if (vet == null) return;
                String reason = askText("Reason for visit:", "New Appointment"); if (reason == null) return;
                try {
                    FileManager.saveManagementRecord("appointments.csv", new String[]{LocalDateTime.now().toString(), pet, owner, phone, date.trim(), time, vet, reason, "Scheduled"});
                    JOptionPane.showMessageDialog(this, "Appointment saved successfully.", "Saved", JOptionPane.INFORMATION_MESSAGE);
                } catch (PetShopException ex) { showError(ex.getMessage()); }
            } else if (choice == 1) {
                try { showTableDialog("Appointment Records", new String[]{"Created", "Pet / ID", "Owner", "Phone", "Date", "Time", "Veterinarian", "Reason", "Status"}, FileManager.loadManagementRecords("appointments.csv", 9), "Saved appointments: " + FileManager.loadManagementRecords("appointments.csv", 9).size(), 1250); }
                catch (PetShopException ex) { showError(ex.getMessage()); }
            }
        }
        private void manageMedicalHistory() {
            String[] options = {"Add Medical Record", "View Medical History", "Cancel"};
            int choice = JOptionPane.showOptionDialog(this, "Keep a record of clinic visits and treatment", "Pet Medical History",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
            if (choice == 0) {
                String pet = askText("Pet name / ID:", "Medical Record"); if (pet == null) return;
                String owner = askText("Owner name:", "Medical Record"); if (owner == null) return;
                String diagnosis = askText("Diagnosis / symptoms:", "Medical Record"); if (diagnosis == null) return;
                String treatment = askText("Treatment / medicine:", "Medical Record"); if (treatment == null) return;
                String vet = askText("Veterinarian / staff:", "Medical Record"); if (vet == null) return;
                String follow = askText("Follow-up date (or N/A):", "Medical Record"); if (follow == null) return;
                try {
                    FileManager.saveManagementRecord("medical_history.csv", new String[]{LocalDate.now().toString(), pet, owner, diagnosis, treatment, vet, follow});
                    JOptionPane.showMessageDialog(this, "Medical history saved successfully.", "Saved", JOptionPane.INFORMATION_MESSAGE);
                } catch (PetShopException ex) { showError(ex.getMessage()); }
            } else if (choice == 1) {
                try { showTableDialog("Pet Medical History", new String[]{"Date", "Pet / ID", "Owner", "Diagnosis / Symptoms", "Treatment / Medicine", "Veterinarian", "Follow-up"}, FileManager.loadManagementRecords("medical_history.csv", 7), "Medical records: " + FileManager.loadManagementRecords("medical_history.csv", 7).size(), 1150); }
                catch (PetShopException ex) { showError(ex.getMessage()); }
            }
        }
        private void manageExpenses() {
            String[] options = {"Add Income / Expense", "View Billing & Expenses", "Cancel"};
            int choice = JOptionPane.showOptionDialog(this, "Record clinic income and business expenses", "Billing & Expenses",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
            if (choice == 0) {
                JComboBox<String> kind = new JComboBox<>(new String[]{"Income", "Expense"});
                JTextField category = new JTextField(18), description = new JTextField(18), amount = new JTextField(12);
                JPanel panel = new JPanel(new GridLayout(0, 2, 8, 8));
                panel.add(new JLabel("Type:")); panel.add(kind);
                panel.add(new JLabel("Category (e.g. clinic fee, medicine):")); panel.add(category);
                panel.add(new JLabel("Description:")); panel.add(description);
                panel.add(new JLabel("Amount (Rs):")); panel.add(amount);
                if (JOptionPane.showConfirmDialog(this, panel, "New Transaction", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
                double value;
                try { value = Double.parseDouble(amount.getText().trim()); if (value <= 0) throw new NumberFormatException(); }
                catch (NumberFormatException ex) { JOptionPane.showMessageDialog(this, "Enter a valid amount greater than zero.", "Invalid Amount", JOptionPane.WARNING_MESSAGE); return; }
                try {
                    FileManager.saveManagementRecord("expenses.csv", new String[]{LocalDateTime.now().toString(), String.valueOf(kind.getSelectedItem()), category.getText().trim(), description.getText().trim(), String.format(Locale.US, "%.2f", value), currentUser.getUsername()});
                    JOptionPane.showMessageDialog(this, "Transaction saved successfully.", "Saved", JOptionPane.INFORMATION_MESSAGE);
                } catch (PetShopException ex) { showError(ex.getMessage()); }
            } else if (choice == 1) {
                try {
                    List<String[]> rows = FileManager.loadManagementRecords("expenses.csv", 6);
                    double income = 0, expense = 0;
                    for (String[] row : rows) try { if ("Income".equalsIgnoreCase(row[1])) income += Double.parseDouble(row[4]); else expense += Double.parseDouble(row[4]); } catch (Exception ignored) { }
                    showTableDialog("Billing & Expenses | Income: Rs " + String.format(Locale.US, "%.2f", income) + " | Expenses: Rs " + String.format(Locale.US, "%.2f", expense) + " | Balance: Rs " + String.format(Locale.US, "%.2f", income-expense),
                            new String[]{"Date / Time", "Type", "Category", "Description", "Amount (Rs)", "Recorded By"}, rows, "Transactions: " + rows.size(), 1100);
                } catch (PetShopException ex) { showError(ex.getMessage()); }
            }
        }
        private void showAboutUs() {
            JDialog dialog = new JDialog(this, "About Us | Snooby Wobby", true);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            JPanel root = new JPanel(new BorderLayout(0, 16));
            root.setBackground(Color.WHITE);
            root.setBorder(new EmptyBorder(24, 28, 22, 28));
            JPanel hero = new JPanel(new BorderLayout(14, 0)); hero.setOpaque(false);
            hero.add(new LogoPanel(72), BorderLayout.WEST);
            JPanel heading = new JPanel(new GridLayout(0, 1, 0, 5)); heading.setOpaque(false);
            JLabel name = new JLabel("Snooby Wobby"); name.setFont(new Font("Serif", Font.BOLD, 28)); name.setForeground(NAVY);
            JLabel subtitle = new JLabel("PET SHOP & CLINIC MANAGEMENT SYSTEM"); subtitle.setFont(new Font("SansSerif", Font.BOLD, 11)); subtitle.setForeground(ORANGE);
            heading.add(name); heading.add(subtitle); hero.add(heading, BorderLayout.CENTER); root.add(hero, BorderLayout.NORTH);
            JTextArea aboutText = new JTextArea(
                    "About Us\n\n" +
                            "Snooby Wobby is a pet shop and veterinary clinic management application designed to make everyday animal care and shop operations more organized, reliable, and efficient.\n\n" +
                            "Our system brings essential tasks together in one place, including pet and inventory records, customer details, sales, adoption records, health and vaccination tracking, emergency case registration, appointments, medical history, and income and expense records.\n\n" +
                            "Our aim is to support responsible pet care through clear record keeping, easier access to information, and a user-friendly desktop interface. The application stores its management records locally so staff can access them during routine operations.\n\n" +
                            "OUR COMMITMENT\n" +
                            "We aim to make everyday pet shop and clinic tasks easier to manage while encouraging careful, responsible animal care.\n\n" +
                            "CONTACT INFORMATION\n" +
                            "Email: snoobywobby07@gmail.com\n" +
                            "Phone: 03242525780\n" +
                            "Location: Karachi, Pakistan\n\n" +
                            "Built for organized service, responsible care, and smoother daily management.");
            aboutText.setEditable(false); aboutText.setLineWrap(true); aboutText.setWrapStyleWord(true); aboutText.setFont(new Font("SansSerif", Font.PLAIN, 14)); aboutText.setForeground(new Color(0x34495E)); aboutText.setBackground(new Color(0xF4F6F8)); aboutText.setBorder(new EmptyBorder(16, 16, 16, 16));
            root.add(aboutText, BorderLayout.CENTER);
            JLabel footer = new JLabel("Current user: " + currentUser.getUsername() + "  |  Role: " + currentUser.getRole().getLabel()); footer.setForeground(GREY); footer.setFont(new Font("SansSerif", Font.PLAIN, 12)); root.add(footer, BorderLayout.SOUTH);
            dialog.setContentPane(root); dialog.setSize(620, 520); dialog.setLocationRelativeTo(this); dialog.setVisible(true);
        }
        private void showContactUs() {
            JDialog dialog = new JDialog(this, "Contact Us | Snooby Wobby", true);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            JPanel root = new JPanel(new BorderLayout(0, 18));
            root.setBackground(Color.WHITE);
            root.setBorder(new EmptyBorder(24, 28, 22, 28));
            JPanel hero = new JPanel(new BorderLayout(14, 0));
            hero.setOpaque(false);
            hero.add(new LogoPanel(68), BorderLayout.WEST);
            JPanel heading = new JPanel(new GridLayout(0, 1, 0, 5));
            heading.setOpaque(false);
            JLabel name = new JLabel("Snooby Wobby");
            name.setFont(new Font("Serif", Font.BOLD, 27));
            name.setForeground(NAVY);
            JLabel subtitle = new JLabel("PET SHOP & CLINIC MANAGEMENT SYSTEM");
            subtitle.setFont(new Font("SansSerif", Font.BOLD, 11));
            subtitle.setForeground(ORANGE);
            heading.add(name);
            heading.add(subtitle);
            hero.add(heading, BorderLayout.CENTER);
            root.add(hero, BorderLayout.NORTH);
            JPanel details = new JPanel(new GridLayout(0, 1, 0, 12));
            details.setBackground(new Color(0xF4F6F8));
            details.setBorder(new EmptyBorder(20, 22, 20, 22));
            details.add(createContactRow("Email", "snoobywobby07@gmail.com"));
            details.add(createContactRow("Phone", "03242525780"));
            details.add(createContactRow("Address", "Karachi, Pakistan"));
            details.add(createContactRow("Office Hours", "Monday - Saturday | 9:00 AM - 6:00 PM"));
            root.add(details, BorderLayout.CENTER);
            JLabel note = new JLabel("Snooby Wobby | Caring for pets, supporting their people.");
            note.setFont(new Font("SansSerif", Font.PLAIN, 11));
            note.setForeground(GREY);
            root.add(note, BorderLayout.SOUTH);
            dialog.setContentPane(root);
            dialog.setSize(600, 410);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        }
        private JPanel createContactRow(String labelText, String valueText) {
            JPanel row = new JPanel(new BorderLayout(12, 0));
            row.setOpaque(false);
            JLabel label = new JLabel(labelText);
            label.setFont(new Font("SansSerif", Font.BOLD, 13));
            label.setForeground(NAVY);
            label.setPreferredSize(new Dimension(105, 24));
            JLabel value = new JLabel(valueText);
            value.setFont(new Font("SansSerif", Font.PLAIN, 13));
            value.setForeground(new Color(0x34495E));
            row.add(label, BorderLayout.WEST);
            row.add(value, BorderLayout.CENTER);
            return row;
        }
        /** Builds the menu bar (File, View, Admin). */
        private JMenuBar buildMenuBar() {
            JMenuItem samples = new JMenuItem("Load Sample Data");
            JMenuItem logout = new JMenuItem("Logout");
            JMenuItem exit = new JMenuItem("Exit");
            JMenuItem gallery = new JMenuItem("Category Gallery");
            JMenuItem emergency = new JMenuItem("Register Animal Emergency");
            JMenuItem emergencyCases = new JMenuItem("Emergency Cases / Queue");
            JMenuItem appointments = new JMenuItem("Appointment Management");
            JMenuItem medicalHistory = new JMenuItem("Pet Medical History");
            JMenuItem expenses = new JMenuItem("Billing & Expenses");
            JMenuItem users = new JMenuItem("Manage Users");
            samples.addActionListener(e -> {
                if (allowed(currentUser.getRole().canManageUsers(), "load sample data", "an Admin")) loadSampleData();
            });
            logout.addActionListener(e -> logout());
            exit.addActionListener(e -> System.exit(0));
            gallery.addActionListener(e -> new CategoryGalleryDialog(this, pets, this::showCategory).setVisible(true));
            emergency.addActionListener(e -> registerEmergencyCase());
            emergencyCases.addActionListener(e -> showEmergencyCases());
            appointments.addActionListener(e -> manageAppointments());
            medicalHistory.addActionListener(e -> manageMedicalHistory());
            expenses.addActionListener(e -> manageExpenses());
            users.addActionListener(e -> {
                if (allowed(currentUser.getRole().canManageUsers(), "manage users", "an Admin")) {
                    new UserManagementDialog(this, currentUser).setVisible(true);
                }
            });
            JMenuItem about = new JMenuItem("About Us");
            about.addActionListener(e -> showAboutUs());
            JMenuItem contact = new JMenuItem("Contact Us");
            contact.addActionListener(e -> showContactUs());
            JMenu file = new JMenu("File"), view = new JMenu("View"), admin = new JMenu("Admin"), help = new JMenu("Help");
            file.add(samples);
            file.addSeparator();
            file.add(logout);
            file.add(exit);
            view.add(gallery);
            view.addSeparator();
            view.add(emergency);
            view.add(emergencyCases);
            view.addSeparator();
            view.add(appointments);
            view.add(medicalHistory);
            view.add(expenses);
            admin.add(users);
            help.add(about);
            help.add(contact);
            JMenuBar bar = new JMenuBar();
            bar.add(file);
            bar.add(view);
            bar.add(admin);
            bar.add(help);
            return bar;
        }
        /** Builds the dark top bar with the logo, shop name, user and statistic cards. */
        private JPanel buildHeader() {
            JPanel header = new JPanel(new BorderLayout(28, 0));
            header.setBackground(NAVY);
            header.setBorder(new EmptyBorder(12, 20, 12, 20));
            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            left.setOpaque(false);
            left.add(new LogoPanel(62));
            JPanel names = new JPanel(new GridLayout(2, 1));
            names.setOpaque(false);
            JLabel title = new JLabel("Snooby Wobby");
            title.setFont(new Font("Serif", Font.BOLD, 29));
            title.setForeground(Color.WHITE);
            JLabel tag = new JLabel("<html>Pet Shop & Clinic &nbsp;|&nbsp; <font color='#F5B041'>"
                    + currentUser.getUsername() + " (" + currentUser.getRole().getLabel() + ")</font></html>");
            tag.setForeground(new Color(0xBDC3C7));
            names.add(title);
            names.add(tag);
            left.add(names);
            header.add(left, BorderLayout.WEST);
            JPanel stats = new JPanel(new GridLayout(1, 6, 14, 0));
            stats.setOpaque(false);
            stats.add(makeCard("PETS & ITEMS", lblPets, ORANGE));
            stats.add(makeCard("STOCK VALUE", lblValue, GREEN));
            stats.add(makeCard("LOW STOCK", lblLow, new Color(0xF1C40F)));
            stats.add(makeCard("TOTAL SALES", lblSales, BLUE));
            stats.add(makeCard("INFECTED", lblInfected, RED));
            stats.add(makeCard("VACCINE DUE", lblVaccDue, PURPLE));
            header.add(stats, BorderLayout.EAST);
            return header;
        }
        /** Creates one statistic card: a small caption and a big value. */
        private JPanel makeCard(String caption, JLabel valueLabel, Color accent) {
            JPanel card = new JPanel(new BorderLayout());
            card.setBackground(NAVY_LIGHT);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 4, 0, 0, accent), new EmptyBorder(8, 13, 8, 14)));
            JLabel cap = new JLabel(caption);
            cap.setForeground(new Color(0xBDC3C7));
            cap.setFont(new Font("SansSerif", Font.BOLD, 11));
            valueLabel.setForeground(Color.WHITE);
            valueLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
            card.add(cap, BorderLayout.NORTH);
            card.add(valueLabel, BorderLayout.CENTER);
            return card;
        }
        /** Builds the centre area: search/filter bar and action buttons on top, the pet table below. */
        private JPanel buildCenter() {
            JPanel center = new JPanel(new BorderLayout(0, 8));
            center.setOpaque(false);
            StyledButton btnSearch = new StyledButton("Search", NAVY_LIGHT);
            StyledButton btnAll = new StyledButton("Show All", GREY);
            JLabel lbl = new JLabel("Search:");
            lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
            JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            searchBar.setOpaque(false);
            for (Component comp : new Component[]{lbl, txtSearch, btnSearch, btnAll, cmbTypeFilter, cmbStatusFilter}) {
                searchBar.add(comp);
            }
            JPanel actionsLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            actionsLeft.setOpaque(false);
            actionsLeft.add(btnHistory);
            actionsLeft.add(btnAdoptions);
            actionsLeft.add(btnCustomers);
            actionsLeft.add(btnEmergencyHistory);
            JPanel actionsRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            actionsRight.setOpaque(false);
            actionsRight.add(btnLogout);
            JPanel actionBar = new JPanel(new BorderLayout());
            actionBar.setOpaque(false);
            actionBar.add(actionsLeft, BorderLayout.WEST);
            actionBar.add(actionsRight, BorderLayout.EAST);
            JPanel topBar = new JPanel(new GridLayout(2, 1, 0, 6));
            topBar.setOpaque(false);
            topBar.add(searchBar);
            topBar.add(actionBar);
            center.add(topBar, BorderLayout.NORTH);
            styleTable(table, (col, value) -> {
                String text = String.valueOf(value);
                if (col == COL_STOCK) return Integer.parseInt(text) <= LOW_STOCK_LIMIT ? RED : null;
                return (col == 5 || col == 6) ? statusColor(text) : null;
            });
            int[] widths = {50, 220, 90, 95, 60, 140, 140};
            for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            table.getColumnModel().getColumn(0).setMaxWidth(60);
            JScrollPane scroll = new JScrollPane(table);
            scroll.getViewport().setBackground(Color.WHITE);
            center.add(scroll, BorderLayout.CENTER);
            lblStatus.setFont(new Font("SansSerif", Font.ITALIC, 12));
            center.add(lblStatus, BorderLayout.SOUTH);
            table.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) fillFormFromSelection();
            });
            txtSearch.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { refreshTable(); }
                @Override public void removeUpdate(DocumentEvent e) { refreshTable(); }
                @Override public void changedUpdate(DocumentEvent e) { refreshTable(); }
            });
            btnSearch.addActionListener(e -> refreshTable());
            cmbTypeFilter.addActionListener(e -> refreshTable());
            cmbStatusFilter.addActionListener(e -> refreshTable());
            btnAll.addActionListener(e -> {
                txtSearch.setText("");
                cmbTypeFilter.setSelectedIndex(0);
                cmbStatusFilter.setSelectedIndex(0);
                refreshTable();
            });
            btnHistory.addActionListener(e -> showSalesHistory());
            btnAdoptions.addActionListener(e -> showAdoptionHistory());
            btnCustomers.addActionListener(e -> showCustomers());
            btnEmergencyHistory.addActionListener(e -> showEmergencyCases());
            btnLogout.addActionListener(e -> logout());
            return center;
        }
        /** Builds the right-hand panel: photo, input fields and action buttons. */
        private JScrollPane buildFormPanel() {
            JPanel form = new JPanel();
            form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
            form.setBackground(Color.WHITE);
            form.setBorder(new EmptyBorder(12, 14, 12, 14));
            JLabel heading = new JLabel("Pet / Item Details");
            heading.setFont(new Font("Serif", Font.BOLD, 20));
            heading.setForeground(NAVY);
            form.add(heading);
            form.add(Box.createVerticalStrut(8));
            photoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            photoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
            form.add(photoPanel);
            form.add(Box.createVerticalStrut(6));
            btnPhoto.setAlignmentX(Component.LEFT_ALIGNMENT);
            btnPhoto.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
            form.add(btnPhoto);
            form.add(Box.createVerticalStrut(8));
            cmbType.setEditable(true);
            cmbType.setSelectedItem("");
            cmbType.addActionListener(e -> {
                if (table.getSelectedRow() < 0 && pendingPhoto == null) photoPanel.showCategory(getTypeText());
            });
            addField(form, "Pet / Item Name", txtName);
            addField(form, "Type / Category", cmbType);
            addField(form, "Price (Rs)", txtPrice);
            addField(form, "Quantity in Stock", txtQty);
            addButtons(form, btnAdd, btnUpdate);
            addButtons(form, btnDelete, btnSell);
            addButtons(form, btnAdopt, btnClear);
            addButtons(form, btnHealth);
            addButtons(form, btnEmergency);
            btnEmergency.setToolTipText("Register an urgent animal case before continuing with routine management");
            btnEmergency.addActionListener(e -> registerEmergencyCase());
            btnPhoto.addActionListener(e -> choosePhoto());
            btnAdd.addActionListener(e -> addPet());
            btnUpdate.addActionListener(e -> updatePet());
            btnDelete.addActionListener(e -> deletePet());
            btnSell.addActionListener(e -> sellPet());
            btnAdopt.addActionListener(e -> adoptPet());
            btnHealth.addActionListener(e -> openHealthDialog());
            btnClear.addActionListener(e -> clearForm());
            JScrollPane scroll = new JScrollPane(form);
            scroll.setBorder(BorderFactory.createLineBorder(new Color(0xDDE1E5)));
            scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.setPreferredSize(new Dimension(300, 100));
            return scroll;
        }
        /** Adds a caption + input component pair to the form panel. */
        private void addField(JPanel form, String caption, JComponent field) {
            JLabel l = new JLabel(caption);
            l.setFont(new Font("SansSerif", Font.BOLD, 12));
            l.setAlignmentX(Component.LEFT_ALIGNMENT);
            field.setAlignmentX(Component.LEFT_ALIGNMENT);
            field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
            form.add(l);
            form.add(Box.createVerticalStrut(3));
            form.add(field);
            form.add(Box.createVerticalStrut(7));
        }
        /** Adds a row of one or two equally wide buttons to the form panel. */
        private void addButtons(JPanel form, StyledButton... buttons) {
            JPanel row = new JPanel(new GridLayout(1, buttons.length, 6, 0));
            row.setOpaque(false);
            for (StyledButton b : buttons) row.add(b);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            form.add(row);
            form.add(Box.createVerticalStrut(6));
        }
        /** Marks the buttons the logged-in role may not use. */
        private void applyPermissions() {
            Role r = currentUser.getRole();
            restrict(r.canManageStock(), btnAdd, btnUpdate, btnPhoto, btnSell, btnAdopt);
            restrict(r.canDelete(), btnDelete);
            restrict(r.canViewSales(), btnHistory, btnAdoptions, btnCustomers);
        }
        /** Paints buttons pale (with a tooltip) when the role is not allowed to use them. */
        private void restrict(boolean allowed, StyledButton... buttons) {
            for (StyledButton b : buttons) {
                b.setRestricted(!allowed);
                if (!allowed) b.setToolTipText("Not allowed for the " + currentUser.getRole().getLabel() + " role");
            }
        }
        /** Returns true when the action is permitted; otherwise explains clearly why not. */
        private boolean allowed(boolean permitted, String action, String who) {
            if (!permitted) {
                JOptionPane.showMessageDialog(this, "Access denied.\nYour role (" + currentUser.getRole().getLabel()
                                + ") is not allowed to " + action + ".\nPlease log in as " + who + " for this.",
                        "Access Denied", JOptionPane.WARNING_MESSAGE);
            }
            return permitted;
        }
        /** Loads pets and vaccination records from file when the window opens. */
        private void loadData() {
            try {
                pets = FileManager.loadPets();
                vaccinations = FileManager.loadVaccinations();
            } catch (PetShopException e) {
                pets = new ArrayList<>();
                vaccinations = new ArrayList<>();
                showError(e.getMessage());
            }
            refreshTable();
        }
        /** Rebuilds the table rows using the search text and both filters, then updates the cards. */
        private void refreshTable() {
            model.setRowCount(0);
            String text = txtSearch.getText().trim().toLowerCase();
            String typeFilter = String.valueOf(cmbTypeFilter.getSelectedItem());
            String statusFilter = String.valueOf(cmbStatusFilter.getSelectedItem());
            int shown = 0;
            for (Pet p : pets) {
                String vacc = Vaccination.overallStatus(p, vaccinations);
                boolean textOk = text.isEmpty() || p.getName().toLowerCase().contains(text)
                        || p.getType().toLowerCase().contains(text);
                boolean typeOk = typeFilter.startsWith("All") || p.getType().equalsIgnoreCase(typeFilter);
                boolean statusOk = statusFilter.startsWith("All") || p.getHealth().equalsIgnoreCase(statusFilter)
                        || vacc.equalsIgnoreCase(statusFilter);
                if (textOk && typeOk && statusOk) {
                    model.addRow(new Object[]{p.getId(), p.getName(), p.getType(),
                            String.format("%.2f", p.getPrice()), p.getQuantity(), p.getHealth(), vacc});
                    shown++;
                }
            }
            if (pets.isEmpty()) {
                lblStatus.setText("No pets or items yet. Fill the form and click 'Add Pet' (or File > Load Sample Data).");
            } else if (shown == 0) {
                lblStatus.setText("No match found. Click 'Show All' to reset the search and filters.");
            } else {
                lblStatus.setText("Showing " + shown + " of " + pets.size() + " pets / items");
            }
            updateStats();
        }
        /** Recalculates the six statistic cards shown in the header. */
        private void updateStats() {
            double value = 0, sales = 0;
            int low = 0, infected = 0, due = 0;
            for (Pet p : pets) {
                value += p.getPrice() * p.getQuantity();
                if (p.getQuantity() <= LOW_STOCK_LIMIT) low++;
                if (p.getHealth().equals("Infected")) infected++;
                String v = Vaccination.overallStatus(p, vaccinations);
                if (v.equals("Overdue") || v.equals("Due Soon")) due++;
            }
            try {
                sales = FileManager.getTotalSales();
            } catch (PetShopException e) {
                showError(e.getMessage());
            }
            lblPets.setText(String.valueOf(pets.size()));
            lblValue.setText(String.format("Rs %,.0f", value));
            lblLow.setText(String.valueOf(low));
            lblSales.setText(String.format("Rs %,.0f", sales));
            lblInfected.setText(String.valueOf(infected));
            lblVaccDue.setText(String.valueOf(due));
        }
        /** Returns the text currently shown in the (editable) type drop-down. */
        private String getTypeText() {
            Object item = cmbType.getEditor().getItem();
            return item == null ? "" : item.toString().trim().replace(",", " ");
        }
        /** Validates the form and returns a Pet with the given id. */
        private Pet readForm(int id) throws PetShopException {
            String name = clean(txtName.getText());
            String type = getTypeText();
            if (name.isEmpty() || type.isEmpty()) throw new PetShopException("Name and type cannot be empty.");
            if (name.length() > 40) throw new PetShopException("Name is too long (maximum 40 characters).");
            double price;
            int qty;
            try {
                price = Double.parseDouble(txtPrice.getText().trim());
                qty = Integer.parseInt(txtQty.getText().trim());
            } catch (NumberFormatException e) {
                throw new PetShopException("Price must be a number and quantity must be a whole number.", e);
            }
            if (price <= 0) throw new PetShopException("Price must be greater than zero.");
            if (qty < 0) throw new PetShopException("Quantity cannot be negative.");
            return new Pet(id, name, type, price, qty, Pet.defaultHealth(type, ""), "");
        }
        /** Throws an exception when another pet already uses this name. */
        private void checkDuplicateName(String name, int ignoreId) throws PetShopException {
            for (Pet p : pets) {
                if (p.getId() != ignoreId && p.getName().equalsIgnoreCase(name)) {
                    throw new PetShopException("A pet or item named '" + name + "' already exists.");
                }
            }
        }
        /** Adds a new pet with an automatically generated ID (and the chosen photo) and saves it. */
        private void addPet() {
            if (!allowed(currentUser.getRole().canManageStock(), "add pets or items", "Admin or Staff")) return;
            try {
                int nextId = 1;
                for (Pet p : pets) nextId = Math.max(nextId, p.getId() + 1);
                Pet pet = readForm(nextId);
                checkDuplicateName(pet.getName(), -1);
                if (pendingPhoto != null) pet.setPhoto(FileManager.savePhoto(pendingPhoto, nextId));
                pets.add(pet);
                FileManager.savePets(pets);
                refreshTable();
                clearForm();
                showInfo("Added successfully.");
            } catch (PetShopException e) {
                showError(e.getMessage());
            }
        }
        /** Updates the pet selected in the table with the values in the form. */
        private void updatePet() {
            if (!allowed(currentUser.getRole().canManageStock(), "update pets or items", "Admin or Staff")) return;
            try {
                Pet selected = getSelectedPet();
                Pet edited = readForm(selected.getId());
                checkDuplicateName(edited.getName(), selected.getId());
                selected.setName(edited.getName());
                selected.setType(edited.getType());
                selected.setPrice(edited.getPrice());
                selected.setQuantity(edited.getQuantity());
                selected.setHealth(Pet.defaultHealth(selected.getType(), selected.getHealth()));
                FileManager.savePets(pets);
                refreshTable();
                showInfo("Updated successfully.");
            } catch (PetShopException e) {
                showError(e.getMessage());
            }
        }
        /** Deletes the selected pet (with its vaccinations and photo) after the user confirms. */
        private void deletePet() {
            if (!allowed(currentUser.getRole().canDelete(), "delete pets or items", "an Admin")) return;
            try {
                Pet selected = getSelectedPet();
                if (JOptionPane.showConfirmDialog(this, "Delete '" + selected.getName() + "' and all its health records?",
                        "Confirm Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    pets.remove(selected);
                    vaccinations.removeIf(v -> v.getPetId() == selected.getId());
                    FileManager.savePets(pets);
                    FileManager.saveVaccinations(vaccinations);
                    FileManager.deletePhoto(selected.getPhoto());
                    refreshTable();
                    clearForm();
                }
            } catch (PetShopException e) {
                showError(e.getMessage());
            }
        }
        /**
         * Health rules shared by selling and adopting: sick animals are refused,
         * and a missing / overdue vaccination needs the user's confirmation.
         *
         * @return false when the user decided to cancel
         */
        private boolean passesAnimalRules(Pet pet, String action) throws PetShopException {
            if (!pet.isAnimal()) return true;
            String health = pet.getHealth();
            if (health.equals("Infected") || health.equals("Under Treatment") || health.equals("Quarantine")) {
                throw new PetShopException("'" + pet.getName() + "' is marked '" + health + "' and cannot be "
                        + action + " until it is Healthy or Recovered.");
            }
            String vacc = Vaccination.overallStatus(pet, vaccinations);
            if (vacc.equals("Not Vaccinated") || vacc.equals("Overdue")) {
                return JOptionPane.showConfirmDialog(this, "The vaccination status of this pet is '" + vacc
                                + "'.\nDo you still want to continue?", "Vaccination Warning",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
            }
            return true;
        }
        /** Shows the buyer / adopter form and returns the validated details, or null when cancelled. */
        private Deal askDeal(Pet pet, boolean adoption) {
            JTextField txtCustomer = new JTextField(), txtPhone = new JTextField(), txtAddress = new JTextField(),
                    txtFee = new JTextField("0"), txtAmount = new JTextField("1");
            JPanel panel = new JPanel(new GridLayout(0, 1, 4, 4));
            panel.add(new JLabel(adoption ? "Adopter name *" : "Customer name *"));
            panel.add(txtCustomer);
            panel.add(new JLabel("Phone number *"));
            panel.add(txtPhone);
            panel.add(new JLabel("Address *"));
            panel.add(txtAddress);
            if (adoption) {
                panel.add(new JLabel("Adoption fee in Rs (0 = free)"));
                panel.add(txtFee);
            } else {
                panel.add(new JLabel("Quantity to sell (in stock: " + pet.getQuantity() + ")"));
                panel.add(txtAmount);
            }
            while (true) {
                if (JOptionPane.showConfirmDialog(this, panel, (adoption ? "Adopt '" : "Sell '") + pet.getName() + "'",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return null;
                try {
                    Deal d = new Deal();
                    d.customer = clean(txtCustomer.getText());
                    if (d.customer.length() < 2 || d.customer.length() > 40) {
                        throw new PetShopException("Name must be 2-40 characters long.");
                    }
                    d.phone = clean(txtPhone.getText());
                    if (!d.phone.matches("[0-9+\\- ]{7,15}")) {
                        throw new PetShopException("Phone number is required: 7-15 digits (you may use + - or spaces).");
                    }
                    d.address = clean(txtAddress.getText());
                    if (d.address.length() < 5 || d.address.length() > 80) {
                        throw new PetShopException("Address must be 5-80 characters long.");
                    }
                    try {
                        if (adoption) {
                            d.fee = Double.parseDouble(txtFee.getText().trim());
                            if (d.fee < 0) throw new PetShopException("Fee cannot be negative.");
                        } else {
                            d.qty = Integer.parseInt(txtAmount.getText().trim());
                            if (d.qty <= 0) throw new PetShopException("Quantity must be at least 1.");
                            if (d.qty > pet.getQuantity()) throw new PetShopException("Only " + pet.getQuantity() + " in stock.");
                        }
                    } catch (NumberFormatException e) {
                        throw new PetShopException("Please enter a valid number in the fee / quantity field.", e);
                    }
                    return d;
                } catch (PetShopException e) {
                    showError(e.getMessage());
                }
            }
        }
        /** Sells the selected pet or item: health rules, buyer details, stock reduction and sale log. */
        private void sellPet() {
            if (!allowed(currentUser.getRole().canManageStock(), "sell pets or items", "Admin or Staff")) return;
            try {
                Pet selected = getSelectedPet();
                if (selected.getQuantity() < 1) throw new PetShopException("'" + selected.getName() + "' is out of stock.");
                if (!passesAnimalRules(selected, "sold")) return;
                Deal d = askDeal(selected, false);
                if (d == null) return;
                FileManager.recordSale(selected, d.qty, currentUser.getUsername(), d.customer, d.phone, d.address);
                selected.setQuantity(selected.getQuantity() - d.qty);
                FileManager.savePets(pets);
                refreshTable();
                showInfo(String.format("Sold %d x %s to %s = Rs %.2f", d.qty, selected.getName(),
                        d.customer, d.qty * selected.getPrice()));
            } catch (PetShopException e) {
                showError(e.getMessage());
            }
        }
        /** Adopts out one animal: same health rules, adopter details, stock -1 and adoption log. */
        private void adoptPet() {
            if (!allowed(currentUser.getRole().canManageStock(), "adopt out pets", "Admin or Staff")) return;
            try {
                Pet selected = getSelectedPet();
                if (!selected.isAnimal()) throw new PetShopException("Only animals can be adopted. Please select a pet.");
                if (selected.getQuantity() < 1) throw new PetShopException("'" + selected.getName() + "' is out of stock.");
                if (!passesAnimalRules(selected, "adopted")) return;
                Deal d = askDeal(selected, true);
                if (d == null) return;
                FileManager.recordAdoption(selected, d.customer, d.phone, d.address, d.fee, currentUser.getUsername());
                selected.setQuantity(selected.getQuantity() - 1);
                FileManager.savePets(pets);
                refreshTable();
                showInfo("'" + selected.getName() + "' was adopted by " + d.customer
                        + (d.fee > 0 ? String.format(" (fee Rs %.2f).", d.fee) : " (free adoption)."));
            } catch (PetShopException e) {
                showError(e.getMessage());
            }
        }
        /** Registers an emergency case so urgent animals can be triaged before routine work. */
        private void registerEmergencyCase() {
            JTextField animal = new JTextField(getSelectedPetNameOrBlank(), 18);
            JComboBox<String> type = new JComboBox<>(new String[]{"Dog", "Cat", "Bird", "Rabbit", "Fish", "Hamster", "Reptile", "Other"});
            JTextField owner = new JTextField(18);
            JTextField phone = new JTextField(18);
            JComboBox<String> urgency = new JComboBox<>(new String[]{"CRITICAL - Immediate", "URGENT - As soon as possible", "Priority - Needs prompt review"});
            JTextArea symptoms = new JTextArea(3, 22);
            JTextArea notes = new JTextArea(2, 22);
            symptoms.setLineWrap(true); symptoms.setWrapStyleWord(true);
            notes.setLineWrap(true); notes.setWrapStyleWord(true);
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4, 5, 4, 5); c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
            int row = 0;
            addEmergencyField(panel, c, row++, "Animal name / ID", animal);
            addEmergencyField(panel, c, row++, "Animal type", type);
            addEmergencyField(panel, c, row++, "Owner / caller name", owner);
            addEmergencyField(panel, c, row++, "Contact number", phone);
            addEmergencyField(panel, c, row++, "Priority", urgency);
            addEmergencyField(panel, c, row++, "Symptoms / reason for visit", new JScrollPane(symptoms));
            addEmergencyField(panel, c, row++, "Additional notes", new JScrollPane(notes));
            JOptionPane option = new JOptionPane(panel, JOptionPane.WARNING_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
            JDialog dialog = option.createDialog(this, "Animal Emergency Registration");
            dialog.setVisible(true);
            Object result = option.getValue();
            dialog.dispose();
            if (!(result instanceof Integer) || ((Integer) result) != JOptionPane.OK_OPTION) return;
            if (animal.getText().trim().isEmpty() || symptoms.getText().trim().isEmpty()) {
                showError("Please enter the animal name/ID and the symptoms or reason for the emergency.");
                return;
            }
            try {
                FileManager.recordEmergency(animal.getText().trim(), String.valueOf(type.getSelectedItem()),
                        owner.getText().trim(), phone.getText().trim(), String.valueOf(urgency.getSelectedItem()),
                        symptoms.getText().trim(), notes.getText().trim(), currentUser.getUsername());
                JOptionPane.showMessageDialog(this,
                        "Emergency case registered and saved.\nPriority: " + urgency.getSelectedItem()
                                + "\n\nIf the animal appears to be in immediate danger, contact a veterinarian/emergency clinic now.",
                        "Emergency Case Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (PetShopException ex) {
                showError("Could not save the emergency case: " + ex.getMessage());
            }
        }
        private String getSelectedPetNameOrBlank() {
            int row = table.getSelectedRow();
            if (row < 0) return "";
            Object value = model.getValueAt(table.convertRowIndexToModel(row), 1);
            return value == null ? "" : value.toString();
        }
        private void addEmergencyField(JPanel panel, GridBagConstraints c, int row, String label, JComponent field) {
            c.gridx = 0; c.gridy = row; c.weightx = 0; c.gridwidth = 1;
            panel.add(new JLabel(label + ":"), c);
            c.gridx = 1; c.weightx = 1;
            panel.add(field, c);
        }
        /** Opens the health and vaccination record of the selected animal. */
        private void openHealthDialog() {
            try {
                Pet selected = getSelectedPet();
                if (!selected.isAnimal()) {
                    throw new PetShopException("Health and vaccination records are only for animals. Please select a pet.");
                }
                new HealthRecordDialog(this, selected, vaccinations, currentUser.getRole().canEditHealth(),
                        currentUser.getUsername(), () -> {
                    try {
                        FileManager.savePets(pets);
                    } catch (PetShopException e) {
                        showError(e.getMessage());
                    }
                    refreshTable();
                    selectRowOfPet(selected.getId());
                }).setVisible(true);
            } catch (PetShopException e) {
                showError(e.getMessage());
            }
        }
        /** Lets the user pick a photo: saved at once for a selected pet, or kept for the next "Add". */
        private void choosePhoto() {
            if (!allowed(currentUser.getRole().canManageStock(), "change photos", "Admin or Staff")) return;
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Images (JPG, PNG, GIF, BMP)",
                    "jpg", "jpeg", "jfif", "png", "gif", "bmp"));
            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            File file = chooser.getSelectedFile();
            try {
                if (table.getSelectedRow() >= 0) {
                    Pet selected = getSelectedPet();
                    String old = selected.getPhoto();
                    selected.setPhoto(FileManager.savePhoto(file, selected.getId()));
                    FileManager.savePets(pets);
                    FileManager.deletePhoto(old);
                    photoPanel.setImage(FileManager.loadPhoto(selected.getPhoto()));
                    showInfo("Photo saved for '" + selected.getName() + "'.");
                } else {
                    BufferedImage preview = ImageIO.read(file);
                    if (preview == null) throw new PetShopException("The selected file is not a valid image.");
                    pendingPhoto = file;
                    photoPanel.setImage(preview);
                }
            } catch (IOException e) {
                showError("Could not read the image file.");
            } catch (PetShopException e) {
                showError(e.getMessage());
            }
        }
        /** Demo records: name, type, price, stock, health. */
        private static final String[][] SAMPLE_PETS = {
                {"Bruno (Labrador)", "Dog", "25000", "3", "Healthy"},
                {"Rocky (German Shepherd)", "Dog", "30000", "2", "Healthy"},
                {"Bella (Golden Retriever)", "Dog", "35000", "4", "Healthy"},
                {"Mittu (Persian)", "Cat", "18000", "4", "Healthy"},
                {"Kitty (Siamese)", "Cat", "15000", "2", "Under Treatment"},
                {"Tota (Parrot)", "Bird", "6000", "8", "Healthy"},
                {"Mithu (Cockatiel)", "Bird", "4500", "5", "Infected"},
                {"Lovebirds Pair", "Bird", "3500", "10", "Healthy"},
                {"Goldfish", "Fish", "300", "40", "Healthy"},
                {"Betta Fish", "Fish", "800", "12", "Healthy"},
                {"Syrian Hamster", "Hamster", "900", "10", "Healthy"},
                {"Bearded Dragon", "Reptile", "12000", "3", "Healthy"},
                {"Rabbit (White)", "Rabbit", "2500", "6", "Quarantine"},
                {"Dog Food 5kg", "Food", "2500", "20", "N/A"},
                {"Cat Food 2kg", "Food", "1800", "15", "N/A"},
                {"Fish Food", "Food", "250", "25", "N/A"},
                {"Pet Collar", "Accessories", "450", "15", "N/A"},
                {"Dog Leash", "Accessories", "600", "12", "N/A"},
                {"Pet Bed", "Accessories", "2200", "4", "N/A"},
                {"Bird Cage", "Accessories", "3500", "3", "N/A"}
        };
        /** Demo vaccinations: pet name, vaccine, days since given, days until next dose is due. */
        private static final String[][] SAMPLE_VACCINES = {
                {"Bruno (Labrador)", "Rabies", "90", "275"},
                {"Bruno (Labrador)", "DHPP", "380", "-15"},
                {"Rocky (German Shepherd)", "Rabies", "30", "335"},
                {"Rocky (German Shepherd)", "DHPP", "30", "335"},
                {"Bella (Golden Retriever)", "Rabies", "340", "25"},
                {"Mittu (Persian)", "Rabies", "60", "305"},
                {"Mittu (Persian)", "FVRCP", "60", "305"},
                {"Tota (Parrot)", "Polyomavirus", "100", "265"},
                {"Lovebirds Pair", "Newcastle Disease", "20", "345"},
                {"Rabbit (White)", "Myxomatosis", "200", "165"}
        };
        /** Adds demo pets, items and vaccination records. Names that already exist are skipped. */
        private void loadSampleData() {
            try {
                int added = 0, nextId = 1;
                for (Pet p : pets) nextId = Math.max(nextId, p.getId() + 1);
                Set<Integer> hadRecords = new HashSet<>();
                for (Vaccination v : vaccinations) hadRecords.add(v.getPetId());
                for (String[] row : SAMPLE_PETS) {
                    if (findPetByName(row[0]) == null) {
                        pets.add(new Pet(nextId++, row[0], row[1], Double.parseDouble(row[2]),
                                Integer.parseInt(row[3]), row[4], ""));
                        added++;
                    }
                }
                LocalDate today = LocalDate.now();
                for (String[] row : SAMPLE_VACCINES) {
                    Pet pet = findPetByName(row[0]);
                    if (pet != null && !hadRecords.contains(pet.getId())) {
                        vaccinations.add(new Vaccination(pet.getId(), row[1], today.minusDays(Long.parseLong(row[2])),
                                today.plusDays(Long.parseLong(row[3])), currentUser.getUsername()));
                    }
                }
                FileManager.savePets(pets);
                FileManager.saveVaccinations(vaccinations);
                refreshTable();
                showInfo(added + " sample pet(s) / item(s) added.");
            } catch (PetShopException | NumberFormatException e) {
                showError("Could not load sample data: " + e.getMessage());
            }
        }
        /** Shows a read-only table in a dialog with a footer line. */
        private void showTableDialog(String title, String[] columns, List<String[]> rows, String footer, int width) {
            ReadOnlyModel m = new ReadOnlyModel(columns);
            for (String[] r : rows) m.addRow(r);
            JTable t = new JTable(m);
            styleTable(t, null);
            JLabel lblFooter = new JLabel("  " + footer);
            lblFooter.setFont(new Font("SansSerif", Font.BOLD, 14));
            lblFooter.setBorder(new EmptyBorder(10, 10, 10, 10));
            JDialog dialog = new JDialog(this, title, true);
            applyIcon(dialog);
            dialog.add(new JScrollPane(t), BorderLayout.CENTER);
            dialog.add(lblFooter, BorderLayout.SOUTH);
            dialog.setSize(width, 440);
            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        }
        /** Displays the saved emergency cases, newest first, so staff can see which animals need attention. */
        private void showEmergencyCases() {
            try {
                List<String[]> cases = FileManager.loadEmergencyCases();
                long critical = cases.stream().filter(r -> "Critical".equalsIgnoreCase(r[5])).count();
                long urgent = cases.stream().filter(r -> "Urgent".equalsIgnoreCase(r[5])).count();
                showTableDialog("Emergency Cases / Queue",
                        new String[]{"Date & Time", "Animal Name / ID", "Type", "Owner / Caller",
                                "Contact", "Priority", "Symptoms / Reason", "Notes", "Registered By"},
                        cases,
                        "Cases: " + cases.size() + "     Critical: " + critical + "     Urgent: " + urgent
                                + "     Newest cases are shown first.", 1250);
            } catch (PetShopException ex) {
                showError("Could not load emergency cases: " + ex.getMessage());
            }
        }
        /** Every recorded sale (with customer name and phone) and the total revenue. */
        private void showSalesHistory() {
            if (!allowed(currentUser.getRole().canViewSales(), "view the sales history", "Admin or Staff")) return;
            try {
                List<String[]> sales = FileManager.loadSales();
                double total = 0;
                for (String[] s : sales) total += Double.parseDouble(s[4]);
                showTableDialog("Sales History", new String[]{"Date & Time", "Item", "Qty", "Unit Price (Rs)",
                                "Total (Rs)", "Sold By", "Customer", "Phone", "Address"}, sales,
                        String.format("Transactions: %d      Total Revenue: Rs %,.2f", sales.size(), total), 1150);
            } catch (PetShopException e) {
                showError(e.getMessage());
            }
        }
        /** Every adoption (adopter, phone, address, fee) and the adoption fees collected. */
        private void showAdoptionHistory() {
            if (!allowed(currentUser.getRole().canViewSales(), "view the adoption history", "Admin or Staff")) return;
            try {
                List<String[]> list = FileManager.loadAdoptions();
                double fees = 0;
                for (String[] a : list) fees += Double.parseDouble(a[6]);
                showTableDialog("Adoption History", new String[]{"Date & Time", "Pet", "Type", "Adopted By",
                                "Phone", "Address", "Fee (Rs)", "Handled By"}, list,
                        String.format("Adoptions: %d      Adoption fees collected: Rs %,.2f", list.size(), fees), 1050);
            } catch (PetShopException e) {
                showError(e.getMessage());
            }
        }
        /** Opens the customer list built from the sales and adoption records. */
        private void showCustomers() {
            if (!allowed(currentUser.getRole().canViewSales(), "view the customer list", "Admin or Staff")) return;
            try {
                new CustomersDialog(this, FileManager.loadSales(), FileManager.loadAdoptions()).setVisible(true);
            } catch (PetShopException e) {
                showError(e.getMessage());
            }
        }
        /** Asks for confirmation, then returns to the login screen. */
        private void logout() {
            if (JOptionPane.showConfirmDialog(this, "Do you want to log out?", "Logout",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                new LoginFrame().setVisible(true);
                dispose();
            }
        }
        /** Shows only one category in the table (called from the Category Gallery). */
        private void showCategory(String type) {
            txtSearch.setText("");
            cmbStatusFilter.setSelectedIndex(0);
            cmbTypeFilter.setSelectedItem(type);
            refreshTable();
        }
        /** Returns the pet with this name (ignoring case), or null when there is none. */
        private Pet findPetByName(String name) {
            for (Pet p : pets) if (p.getName().equalsIgnoreCase(name)) return p;
            return null;
        }
        /** Returns the Pet object matching the selected table row. */
        private Pet getSelectedPet() throws PetShopException {
            int row = table.getSelectedRow();
            if (row < 0) throw new PetShopException("Please select a pet or item from the table first.");
            int id = (int) model.getValueAt(row, 0);
            for (Pet p : pets) if (p.getId() == id) return p;
            throw new PetShopException("The selected pet or item was not found.");
        }
        /** Selects the table row that shows the pet with the given id (if it is visible). */
        private void selectRowOfPet(int id) {
            for (int i = 0; i < model.getRowCount(); i++) {
                if ((int) model.getValueAt(i, 0) == id) {
                    table.setRowSelectionInterval(i, i);
                    return;
                }
            }
        }
        /** Copies the selected row into the form and shows the pet's own photo or its category picture. */
        private void fillFormFromSelection() {
            int row = table.getSelectedRow();
            if (row < 0) return;
            txtName.setText(model.getValueAt(row, 1).toString());
            cmbType.setSelectedItem(model.getValueAt(row, 2).toString());
            txtPrice.setText(model.getValueAt(row, 3).toString());
            txtQty.setText(model.getValueAt(row, 4).toString());
            pendingPhoto = null;
            int id = (int) model.getValueAt(row, 0);
            String photo = "";
            for (Pet p : pets) if (p.getId() == id) photo = p.getPhoto();
            BufferedImage own = FileManager.loadPhoto(photo);
            if (own != null) photoPanel.setImage(own);
            else photoPanel.showCategory(model.getValueAt(row, 2).toString());
        }
        /** Empties all form fields, the photo preview and the table selection. */
        private void clearForm() {
            txtName.setText("");
            cmbType.setSelectedItem("");
            txtPrice.setText("");
            txtQty.setText("");
            pendingPhoto = null;
            photoPanel.setImage(null);
            table.clearSelection();
        }
        private void showError(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }
        private void showInfo(String msg) { JOptionPane.showMessageDialog(this, msg, "Snooby Wobby", JOptionPane.INFORMATION_MESSAGE); }
    }
    /**
     * CustomersDialog - one row per customer, built from the sales and adoption
     * files: purchases, adoptions, total money spent and last activity.
     * The search box filters by name or phone number.
     */
    static class CustomersDialog extends JDialog {
        private static final long serialVersionUID = 1L;
        /** Totals of one customer. */
        private static class Stat {
            String name, phone, address = "-", last = "";
            int purchases, adoptions;
            double spent;
        }
        private final transient Map<String, Stat> stats = new LinkedHashMap<>();
        private final ReadOnlyModel tableModel = new ReadOnlyModel(
                new String[]{"Customer", "Phone", "Address", "Purchases", "Adoptions", "Total Spent (Rs)", "Last Activity"});
        private final JTextField txtSearch = new JTextField(16);
        private final JLabel lblFooter = new JLabel();
        CustomersDialog(Frame owner, List<String[]> sales, List<String[]> adoptions) {
            super(owner, "Customers", true);
            applyIcon(this);
            setSize(1000, 520);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(0, 8));
            for (String[] s : sales) {
                if (s[6].equals("-")) continue; // old records without a customer name
                Stat st = statFor(s[6], s[7]);
                if (!s[8].equals("-")) st.address = s[8];
                st.purchases++;
                st.spent += Double.parseDouble(s[4]);
                if (s[0].compareTo(st.last) > 0) st.last = s[0];
            }
            for (String[] a : adoptions) {
                Stat st = statFor(a[3], a[4]);
                if (!a[5].equals("-")) st.address = a[5];
                st.adoptions++;
                st.spent += Double.parseDouble(a[6]);
                if (a[0].compareTo(st.last) > 0) st.last = a[0];
            }
            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
            top.add(new JLabel("Search name, phone or address:"));
            top.add(txtSearch);
            add(top, BorderLayout.NORTH);
            JTable table = new JTable(tableModel);
            styleTable(table, null);
            JPanel center = new JPanel(new BorderLayout());
            center.setBorder(new EmptyBorder(0, 12, 0, 12));
            center.add(new JScrollPane(table), BorderLayout.CENTER);
            add(center, BorderLayout.CENTER);
            lblFooter.setFont(new Font("SansSerif", Font.BOLD, 14));
            lblFooter.setBorder(new EmptyBorder(8, 14, 10, 10));
            add(lblFooter, BorderLayout.SOUTH);
            txtSearch.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { refresh(); }
                @Override public void removeUpdate(DocumentEvent e) { refresh(); }
                @Override public void changedUpdate(DocumentEvent e) { refresh(); }
            });
            refresh();
        }
        /** Finds (or creates) the totals object of a customer; name + phone identify a customer. */
        private Stat statFor(String name, String phone) {
            return stats.computeIfAbsent(name.toLowerCase() + "|" + phone, k -> {
                Stat st = new Stat();
                st.name = name;
                st.phone = phone;
                return st;
            });
        }
        /** Refills the table using the search text. */
        private void refresh() {
            tableModel.setRowCount(0);
            String text = txtSearch.getText().trim().toLowerCase();
            double total = 0;
            int shown = 0;
            for (Stat s : stats.values()) {
                if (text.isEmpty() || s.name.toLowerCase().contains(text) || s.phone.toLowerCase().contains(text)
                        || s.address.toLowerCase().contains(text)) {
                    tableModel.addRow(new Object[]{s.name, s.phone, s.address, s.purchases, s.adoptions,
                            String.format("%,.2f", s.spent), s.last.isEmpty() ? "-" : s.last});
                    total += s.spent;
                    shown++;
                }
            }
            lblFooter.setText(String.format("Customers: %d      Total spent: Rs %,.2f", shown, total));
        }
    }
    /**
     * HealthRecordDialog - health condition and vaccination history of one pet.
     * A veterinarian (or admin) can change the condition and add or remove
     * vaccinations; other roles can only view the record.
     */
    static class HealthRecordDialog extends JDialog {
        private static final long serialVersionUID = 1L;
        private final transient Pet pet;
        private final transient List<Vaccination> allRecords;
        private final transient Runnable onChange;
        private final String doneBy;
        private final transient List<Vaccination> shownRows = new ArrayList<>();
        private final ReadOnlyModel tableModel = new ReadOnlyModel(
                new String[]{"Vaccine", "Date Given", "Next Due", "Status", "Given By"});
        private final JTable table = new JTable(tableModel);
        private final JComboBox<String> cmbHealth = new JComboBox<>(HEALTH_OPTIONS);
        private final JComboBox<String> cmbVaccine = new JComboBox<>();
        private final JTextField txtGiven = new JTextField(LocalDate.now().toString(), 10);
        private final JTextField txtDue = new JTextField(LocalDate.now().plusYears(1).toString(), 10);
        private final JLabel lblVaccStatus = new JLabel();
        HealthRecordDialog(Frame owner, Pet pet, List<Vaccination> allRecords, boolean canEdit,
                           String doneBy, Runnable onChange) {
            super(owner, "Health & Vaccination Record", true);
            this.pet = pet;
            this.allRecords = allRecords;
            this.onChange = onChange;
            this.doneBy = doneBy;
            applyIcon(this);
            setSize(920, 600);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout());
            JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
            header.setBackground(NAVY);
            header.add(new LogoPanel(46));
            JLabel title = new JLabel("<html><span style='font-size:16pt'>" + pet.getName()
                    + "</span><br><span style='color:#BDC3C7'>" + pet.getType()
                    + " &nbsp;|&nbsp; Health & Vaccination Record</span></html>");
            title.setForeground(Color.WHITE);
            header.add(title);
            add(header, BorderLayout.NORTH);
            JPanel center = new JPanel(new BorderLayout(0, 8));
            center.setBorder(new EmptyBorder(12, 14, 6, 14));
            JPanel conditionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            cmbHealth.setSelectedItem(pet.getHealth());
            StyledButton btnSaveHealth = new StyledButton("Save Condition", NAVY_LIGHT);
            lblVaccStatus.setFont(new Font("SansSerif", Font.BOLD, 13));
            conditionRow.add(new JLabel("Health condition:"));
            conditionRow.add(cmbHealth);
            conditionRow.add(btnSaveHealth);
            conditionRow.add(Box.createHorizontalStrut(20));
            conditionRow.add(lblVaccStatus);
            center.add(conditionRow, BorderLayout.NORTH);
            styleTable(table, (col, value) -> col == 3 ? statusColor(String.valueOf(value)) : null);
            center.add(new JScrollPane(table), BorderLayout.CENTER);
            add(center, BorderLayout.CENTER);
            txtGiven.setEditable(false);
            txtDue.setEditable(false);
            txtGiven.setToolTipText("Automatically set to today by the system");
            txtDue.setToolTipText("Automatically set to one year after the date given");
            JPanel south = new JPanel();
            south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
            JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            cmbVaccine.setModel(new DefaultComboBoxModel<>(vaccineSuggestions(pet.getType())));
            cmbVaccine.setEditable(true);
            cmbVaccine.setPreferredSize(new Dimension(190, 28));
            row1.add(new JLabel("Vaccine:"));
            row1.add(cmbVaccine);
            row1.add(new JLabel("Date given (auto):"));
            row1.add(txtGiven);
            row1.add(new JLabel("Next due (auto +1 year):"));
            row1.add(txtDue);
            JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            StyledButton btnAdd = new StyledButton("Add Vaccination", GREEN);
            StyledButton btnDelete = new StyledButton("Delete Selected Record", RED);
            StyledButton btnClose = new StyledButton("Close", GREY);
            row2.add(btnAdd);
            row2.add(btnDelete);
            row2.add(btnClose);
            south.add(row1);
            south.add(row2);
            add(south, BorderLayout.SOUTH);
            btnSaveHealth.addActionListener(e -> saveCondition());
            btnAdd.addActionListener(e -> addVaccination());
            btnDelete.addActionListener(e -> deleteVaccination());
            btnClose.addActionListener(e -> dispose());
            String note = "Add Vaccination";
            boolean vaccOff = false;
            if (!canEdit) {
                cmbHealth.setEnabled(false);
                btnSaveHealth.setEnabled(false);
                vaccOff = true;
                note = "View only - your role cannot change health records";
            } else if (!pet.needsVaccination()) {
                vaccOff = true;
                note = "No vaccines are needed for this type of animal";
            }
            if (vaccOff) {
                for (JComponent c : new JComponent[]{btnAdd, btnDelete, cmbVaccine, txtGiven, txtDue}) c.setEnabled(false);
            }
            south.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(0, 14, 12, 14),
                    BorderFactory.createTitledBorder(note)));
            refreshRecords();
        }
        /** Common vaccines for each kind of animal (shown in the drop-down). */
        private static String[] vaccineSuggestions(String type) {
            switch (type.toLowerCase()) {
                case "dog": return new String[]{"Rabies", "DHPP", "Bordetella", "Leptospirosis", "Canine Influenza"};
                case "cat": return new String[]{"Rabies", "FVRCP", "Feline Leukemia (FeLV)", "Chlamydia"};
                case "bird": return new String[]{"Polyomavirus", "Newcastle Disease", "Avian Pox"};
                case "rabbit": return new String[]{"Myxomatosis", "RHDV"};
                default: return new String[]{"Rabies", "Other"};
            }
        }
        /** Reloads the vaccination table and the overall status label. */
        private void refreshRecords() {
            tableModel.setRowCount(0);
            shownRows.clear();
            List<Vaccination> mine = Vaccination.forPet(allRecords, pet.getId());
            mine.sort((a, b) -> b.getGiven().compareTo(a.getGiven()));
            Map<String, Vaccination> latest = Vaccination.latestByVaccine(mine);
            for (Vaccination v : mine) {
                boolean isLatest = latest.get(v.getVaccine().toLowerCase()) == v;
                tableModel.addRow(new Object[]{v.getVaccine(), v.getGiven(), v.getNextDue(),
                        isLatest ? Vaccination.statusOf(v.getNextDue()) : "Completed", v.getGivenBy()});
                shownRows.add(v);
            }
            String overall = Vaccination.overallStatus(pet, allRecords);
            lblVaccStatus.setText("Vaccination status: " + overall);
            lblVaccStatus.setForeground(statusColor(overall));
        }
        /** Saves the health condition chosen in the drop-down. */
        private void saveCondition() {
            pet.setHealth(String.valueOf(cmbHealth.getSelectedItem()));
            onChange.run();
            JOptionPane.showMessageDialog(this, "Health condition saved: " + pet.getHealth());
        }
        /** Validates the form and adds a new vaccination record. */
        private void addVaccination() {
            try {
                Object item = cmbVaccine.getEditor().getItem();
                String vaccine = item == null ? "" : clean(item.toString());
                if (vaccine.isEmpty()) throw new PetShopException("Please enter the vaccine name.");
                LocalDate given = LocalDate.now();
                LocalDate due = given.plusYears(1);
                if (!due.isAfter(given)) throw new PetShopException("The next due date must be after the date given.");
                for (Vaccination v : Vaccination.forPet(allRecords, pet.getId())) {
                    if (v.getVaccine().equalsIgnoreCase(vaccine) && v.getGiven().equals(given)) {
                        throw new PetShopException("This vaccination is already recorded for that date.");
                    }
                }
                allRecords.add(new Vaccination(pet.getId(), vaccine, given, due, doneBy));
                FileManager.saveVaccinations(allRecords);
                refreshRecords();
                onChange.run();
                txtGiven.setText(LocalDate.now().toString());
                txtDue.setText(LocalDate.now().plusYears(1).toString());
            } catch (PetShopException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        /** Deletes the selected vaccination record after confirmation. */
        private void deleteVaccination() {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Please select a vaccination record first.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (JOptionPane.showConfirmDialog(this, "Delete this vaccination record?", "Confirm Delete",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            try {
                allRecords.remove(shownRows.get(row));
                FileManager.saveVaccinations(allRecords);
                refreshRecords();
                onChange.run();
            } catch (PetShopException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        /** Converts text such as 2026-10-12 into a date, or throws a friendly exception. */
        private static LocalDate parseDate(String text, String fieldName) throws PetShopException {
            try {
                return LocalDate.parse(text.trim());
            } catch (DateTimeParseException e) {
                throw new PetShopException(fieldName + " must be written as yyyy-MM-dd, for example " + LocalDate.now() + ".", e);
            }
        }
    }
    /**
     * UserManagementDialog - the administrator can see all accounts, create users
     * with a chosen role and delete users (not his own account, not the last admin).
     */
    static class UserManagementDialog extends JDialog {
        private static final long serialVersionUID = 1L;
        private final transient User currentUser;
        private transient List<User> users = new ArrayList<>();
        private final ReadOnlyModel tableModel = new ReadOnlyModel(new String[]{"Username", "Role"});
        private final JTable table = new JTable(tableModel);
        private final JTextField txtUser = new JTextField(12);
        private final JPasswordField txtPass = new JPasswordField(12);
        private final JComboBox<Role> cmbRole = new JComboBox<>(Role.values());
        UserManagementDialog(Frame owner, User currentUser) {
            super(owner, "Manage Users", true);
            this.currentUser = currentUser;
            applyIcon(this);
            setSize(560, 460);
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout(0, 8));
            styleTable(table, null);
            JPanel tablePanel = new JPanel(new BorderLayout());
            tablePanel.setBorder(new EmptyBorder(12, 14, 0, 14));
            tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
            add(tablePanel, BorderLayout.CENTER);
            JPanel form = new JPanel();
            form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
            form.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(0, 14, 12, 14),
                    BorderFactory.createTitledBorder("Create / Delete User")));
            JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            row1.add(new JLabel("Username:"));
            row1.add(txtUser);
            row1.add(new JLabel("Password:"));
            row1.add(txtPass);
            JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            StyledButton btnAdd = new StyledButton("Add User", GREEN);
            StyledButton btnDelete = new StyledButton("Delete Selected", RED);
            StyledButton btnClose = new StyledButton("Close", GREY);
            row2.add(new JLabel("Role:"));
            row2.add(cmbRole);
            row2.add(btnAdd);
            row2.add(btnDelete);
            row2.add(btnClose);
            form.add(row1);
            form.add(row2);
            add(form, BorderLayout.SOUTH);
            btnAdd.addActionListener(e -> addUser());
            btnDelete.addActionListener(e -> deleteUser());
            btnClose.addActionListener(e -> dispose());
            loadUsers();
        }
        /** Loads all users from file into the table. */
        private void loadUsers() {
            try {
                users = FileManager.loadUsers();
            } catch (PetShopException e) {
                users = new ArrayList<>();
                showError(e.getMessage());
            }
            tableModel.setRowCount(0);
            for (User u : users) tableModel.addRow(new Object[]{u.getUsername(), u.getRole().getLabel()});
        }
        /** Validates the form and creates a new account. */
        private void addUser() {
            try {
                String name = txtUser.getText().trim().toLowerCase();
                String password = new String(txtPass.getPassword());
                User.validateNewAccount(name, password, users);
                users.add(new User(name, User.hash(password), (Role) cmbRole.getSelectedItem()));
                FileManager.saveUsers(users);
                txtUser.setText("");
                txtPass.setText("");
                loadUsers();
            } catch (PetShopException e) {
                showError(e.getMessage());
            }
        }
        /** Deletes the selected account, protecting the current user and the last admin. */
        private void deleteUser() {
            try {
                int row = table.getSelectedRow();
                if (row < 0) throw new PetShopException("Please select a user first.");
                User target = users.get(row);
                if (target.getUsername().equalsIgnoreCase(currentUser.getUsername())) {
                    throw new PetShopException("You cannot delete the account you are logged in with.");
                }
                if (target.getRole() == Role.ADMIN) {
                    int admins = 0;
                    for (User u : users) if (u.getRole() == Role.ADMIN) admins++;
                    if (admins <= 1) throw new PetShopException("The last administrator cannot be deleted.");
                }
                if (JOptionPane.showConfirmDialog(this, "Delete user '" + target.getUsername() + "'?",
                        "Confirm Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    users.remove(target);
                    FileManager.saveUsers(users);
                    loadUsers();
                }
            } catch (PetShopException e) {
                showError(e.getMessage());
            }
        }
        private void showError(String msg) { JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE); }
    }
    /**
     * CategoryGalleryDialog - one card for every category with its picture, the
     * number of pets / items and the stock. "Show" filters the main table.
     */
    static class CategoryGalleryDialog extends JDialog {
        private static final long serialVersionUID = 1L;
        CategoryGalleryDialog(Frame owner, List<Pet> pets, Consumer<String> onPick) {
            super(owner, "Category Gallery", true);
            applyIcon(this);
            setSize(1000, 800);
            setMinimumSize(new Dimension(960, 760));
            setLocationRelativeTo(owner);
            setLayout(new BorderLayout());
            JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
            header.setBackground(NAVY);
            header.add(new LogoPanel(46));
            JLabel title = new JLabel("<html><span style='font-size:16pt'>Category Gallery</span><br>"
                    + "<span style='color:#BDC3C7'>Click 'Show' to see the pets of a category</span></html>");
            title.setForeground(Color.WHITE);
            header.add(title);
            add(header, BorderLayout.NORTH);
            String[][] categories = {{"Dogs", "Dog"}, {"Cats", "Cat"}, {"Rabbits", "Rabbit"},
                    {"Birds", "Bird"}, {"Fish", "Fish"}, {"Hamsters", "Hamster"},
                    {"Reptiles", "Reptile"}, {"Pet Food", "Food"}, {"Accessories", "Accessories"},
                    {"Toys", "Toys"}, {"Medicine", "Medicine"}};
            JPanel grid = new JPanel();
            grid.setLayout(new BoxLayout(grid, BoxLayout.Y_AXIS));
            grid.setBackground(BACKGROUND);
            grid.setBorder(new EmptyBorder(14, 14, 14, 14));
            JPanel mainRows = new JPanel(new GridLayout(3, 3, 12, 12));
            mainRows.setBackground(BACKGROUND);
            for (int i = 0; i < 9; i++) mainRows.add(buildCard(categories[i][0], categories[i][1], pets, onPick));
            grid.add(mainRows);
            JPanel lastRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 12));
            lastRow.setBackground(BACKGROUND);
            lastRow.add(buildCard(categories[9][0], categories[9][1], pets, onPick));
            lastRow.add(buildCard(categories[10][0], categories[10][1], pets, onPick));
            grid.add(lastRow);
            JScrollPane galleryScroll = new JScrollPane(grid);
            galleryScroll.setBorder(null);
            galleryScroll.getVerticalScrollBar().setUnitIncrement(18);
            galleryScroll.getViewport().setBackground(BACKGROUND);
            add(galleryScroll, BorderLayout.CENTER);
        }
        /** Makes every gallery thumbnail exactly the same size without cropping faces. */
        private Image makeGalleryThumbnail(BufferedImage source, int width, int height) {
            BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = canvas.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(source, 0, 0, width, height, null);
            g2.dispose();
            return canvas;
        }
        /** Builds one category card: picture, name, counts and a Show button. */
        private JPanel buildCard(String title, String type, List<Pet> pets, Consumer<String> onPick) {
            int count = 0, stock = 0;
            String key = CategoryPhotos.keyFor(type);
            for (Pet p : pets) {
                if (key != null && key.equals(CategoryPhotos.keyFor(p.getType()))) {
                    count++;
                    stock += p.getQuantity();
                }
            }
            JLabel photo = new JLabel("No photo", SwingConstants.CENTER);
            photo.setPreferredSize(new Dimension(200, 120));
            photo.setMinimumSize(new Dimension(200, 120));
            photo.setMaximumSize(new Dimension(200, 120));
            BufferedImage img = CategoryPhotos.get(type);
            if (img != null) {
                photo.setText(null);
                photo.setIcon(new ImageIcon(makeGalleryThumbnail(img, 200, 120)));
            }
            JLabel name = new JLabel(title, SwingConstants.CENTER);
            name.setFont(new Font("Serif", Font.BOLD, 18));
            name.setForeground(NAVY);
            JLabel info = new JLabel(count + " item(s)  |  stock " + stock, SwingConstants.CENTER);
            info.setForeground(GREY);
            StyledButton show = new StyledButton("Show", ORANGE);
            show.addActionListener(e -> {
                dispose();
                onPick.accept(type);
            });
            JPanel text = new JPanel(new GridLayout(2, 1));
            text.setOpaque(false);
            text.add(name);
            text.add(info);
            JPanel bottom = new JPanel(new BorderLayout(0, 6));
            bottom.setOpaque(false);
            bottom.add(text, BorderLayout.CENTER);
            bottom.add(show, BorderLayout.SOUTH);
            JPanel card = new JPanel(new BorderLayout(0, 8));
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xDDE1E5)), new EmptyBorder(10, 10, 10, 10)));
            card.add(photo, BorderLayout.CENTER);
            card.add(bottom, BorderLayout.SOUTH);
            return card;
        }
    }
}

