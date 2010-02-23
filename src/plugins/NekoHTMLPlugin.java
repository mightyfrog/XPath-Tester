package plugins;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;

import org.cyberneko.html.parsers.DOMParser;

import org.mightyfrog.util.xpathtester.Plugin;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author Shigehiro Soejima
 */
public class NekoHTMLPlugin implements Plugin {
    //
    private Charset charset = Charset.defaultCharset();

    /** */
    @Override
    public Document getDocument(URL url) throws Exception {
        Document doc = null;
        Reader reader = null;
        InputStream in = null;
        try {
            in = url.openStream();
            reader = new InputStreamReader(in, getCharset());
            InputSource is = new InputSource(reader);
            DOMParser parser = new DOMParser();
            parser.parse(is);
            doc = parser.getDocument();
        } catch (IOException e) {
            throw e;
        } catch (SAXException e) {
            throw e;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
            if (in != null) {
                try {
                    in.close();
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
        return "CyberNeko HTML Parser Plugin";
    }
}
