package plugins;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import javax.xml.parsers.ParserConfigurationException;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;

import org.mightyfrog.util.xpathtester.Plugin;

import org.w3c.dom.Document;

/**
 *
 * @author Shigehiro Soejima
 */
public class HtmlCleanerPlugin implements Plugin {
    //
    private Charset charset = null;

    /** */
    @Override
    public Document getDocument(URL url) throws Exception {
        HtmlCleaner cleaner = new HtmlCleaner();
        CleanerProperties props = cleaner.getProperties();

        Document doc = null;
        Reader reader = null;
        try {
            reader = new InputStreamReader(url.openStream(), getCharset());
            TagNode node = cleaner.clean(reader);
            doc = new DomSerializer(props, true).createDOM(node);
        } catch (ParserConfigurationException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }

        return doc;
    }

    /** */
    @Override
    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    /** */
    @Override
    public Charset getCharset() {
        return this.charset;
    }

    /** */
    @Override
    public String getName() {
        return "HtmlCleaner Plugin";
    }
}
