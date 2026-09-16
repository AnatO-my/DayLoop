import com.android.ide.common.vectordrawable.Svg2Vector;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

/** Offline asset conversion only; never invokes Gradle or changes original artwork. */
class ImportBrandAssets {
    static final String SVG = "http://www.w3.org/2000/svg";
    static final String ANDROID = "http://schemas.android.com/apk/res/android";
    static Path root;

    static Document read(InputStream input) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        return factory.newDocumentBuilder().parse(input);
    }

    static void write(Document doc, Path path) throws Exception {
        Files.createDirectories(path.getParent());
        var transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new DOMSource(doc), new StreamResult(path.toFile()));
    }

    static Document convert(Document svg) throws Exception {
        // Percentage dimensions from the editor are not intrinsic Android dimensions.
        var element = svg.getDocumentElement();
        var box = element.getAttribute("viewBox").trim().split("[ ,]+");
        element.setAttribute("width", box[2]);
        element.setAttribute("height", box[3]);
        if (svg.getDoctype() != null) svg.removeChild(svg.getDoctype());
        var temporary = Files.createTempFile("dayloop-brand-", ".svg");
        try {
            write(svg, temporary);
            var output = new ByteArrayOutputStream();
            String diagnostics = Svg2Vector.parseSvgToXml(temporary, output);
            if (diagnostics != null && !diagnostics.isBlank()) {
                throw new IOException("SVG conversion requires review: " + diagnostics);
            }
            return read(new ByteArrayInputStream(output.toByteArray()));
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static void attribute(Element element, String name, double value) {
        element.setAttributeNS(ANDROID, "android:" + name, Double.toString(value));
    }

    static Document padded(Document vector, double canvas, double artworkWidth) {
        var element = vector.getDocumentElement();
        double width = Double.parseDouble(element.getAttributeNS(ANDROID, "viewportWidth"));
        double height = Double.parseDouble(element.getAttributeNS(ANDROID, "viewportHeight"));
        double scale = artworkWidth / width;
        var group = vector.createElement("group");
        attribute(group, "scaleX", scale);
        attribute(group, "scaleY", scale);
        attribute(group, "translateX", (canvas - width * scale) / 2);
        attribute(group, "translateY", (canvas - height * scale) / 2);
        while (element.hasChildNodes()) group.appendChild(element.getFirstChild());
        element.appendChild(group);
        attribute(element, "viewportWidth", canvas);
        attribute(element, "viewportHeight", canvas);
        element.setAttributeNS(ANDROID, "android:width", canvas + "dp");
        element.setAttributeNS(ANDROID, "android:height", canvas + "dp");
        return vector;
    }

    static Document source(String name) throws Exception {
        try (var input = Files.newInputStream(root.resolve("icon_files/" + name + ".svg"))) {
            return read(input);
        }
    }

    static void brand(String name, String resource, double canvas, double width) throws Exception {
        var vector = convert(source(name));
        if (canvas > 0) vector = padded(vector, canvas, width);
        write(vector, root.resolve("app/src/main/res/" + resource + ".xml"));
        System.out.println("Imported " + name + " -> " + resource);
    }

    public static void main(String[] args) throws Exception {
        root = Path.of(args[0]).toAbsolutePath().normalize();
        brand("launcher-background", "drawable/dayloop_launcher_background", 108, 108);
        brand("launcher-foreground", "drawable/dayloop_launcher_foreground", 108, 56);
        brand("monochrome", "drawable/dayloop_launcher_monochrome", 108, 56);
        brand("notification-small-icon", "drawable/ic_dayloop_notification", 24, 17);
        brand("no-bck-splash-light", "drawable/dayloop_splash_mark", 288, 160);
        brand("no-bck-splash-dark", "drawable-night/dayloop_splash_mark", 288, 160);
        for (String name : List.of("mascot", "mascot-extra", "wordmark-light", "wordmark-dark")) {
            brand(name, "drawable/dayloop_" + name.replace('-', '_'), 0, 0);
        }

        // Sheet origins preserve the source's 24 x 24 design grid, not tight path bounds.
        String[][] icons = {
            {"Untitled-10.svg", "Calendar / Calendar_Check", "today", "408", "294"},
            {"Untitled-10.svg", "Calendar / Calendar_Days", "plan", "408", "406"},
            {"Untitled-10.svg", "Calendar / Timer", "focus", "408", "462"},
            {"Untitled-8.svg", "Navigation / Flag", "goals", "408", "518"},
            {"Untitled-5.svg", "Menu / More_Horizontal", "more", "96", "518"},
            {"Untitled-1.svg", "Media / Play", "action_play", "96", "462"},
            {"Untitled-1.svg", "Media / Pause", "action_pause", "408", "462"},
            {"Untitled-3.svg", "System / Save", "action_save", "96", "630"},
            {"Untitled-5.svg", "Menu / Close_MD", "action_close", "408", "294"},
            {"Untitled-5.svg", "Menu / Menu_Alt_01", "action_menu", "720", "462"},
            {"Untitled-9.svg", "Edit / Add_Plus", "action_add", "720", "574"},
            {"Untitled-9.svg", "Edit / Undo", "action_reset", "720", "1078"},
            {"Untitled-9.svg", "Edit / Edit_Pencil_01", "action_edit", "96", "1526"},
            {"Untitled-11.svg", "Interface / Check", "action_check", "96", "350"},
            {"Untitled-11.svg", "Interface / Trash_Empty", "action_delete", "408", "1078"},
            {"Untitled-13.svg", "Arrow / Arrow_Left_MD", "action_back", "408", "1078"},
            {"Untitled-13.svg", "Arrow / Arrow_Right_MD", "action_forward", "408", "1134"},
        };
        try (var zip = new ZipFile(root.resolve("icon_files/coolicons _ Free Iconset (Community).zip").toFile())) {
            for (var icon : icons) {
                Document sheet;
                try (var input = zip.getInputStream(zip.getEntry(icon[0]))) { sheet = read(input); }
                Element found = null;
                var groups = sheet.getElementsByTagNameNS(SVG, "g");
                for (int i = 0; i < groups.getLength(); i++) {
                    var group = (Element) groups.item(i);
                    if (group.getAttribute("id").equals(icon[1])) { found = group; break; }
                }
                if (found == null) throw new IOException("Missing supplied icon: " + icon[1]);
                var doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
                var svg = doc.createElementNS(SVG, "svg");
                svg.setAttribute("viewBox", "0 0 24 24");
                svg.setAttribute("fill", "none");
                doc.appendChild(svg);
                var group = doc.createElementNS(SVG, "g");
                group.setAttribute("transform", "translate(-" + icon[3] + " -" + icon[4] + ")");
                group.appendChild(doc.importNode(found, true));
                svg.appendChild(group);
                String resource = icon[2].startsWith("action_") ? "ic_" + icon[2] : "ic_nav_" + icon[2];
                write(convert(doc), root.resolve("app/src/main/res/drawable/" + resource + ".xml"));
                System.out.println("Imported coolicons " + icon[1]);
            }
        }
    }
}
