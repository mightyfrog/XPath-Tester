package plugins;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.charset.Charset;

import org.lobobrowser.html.UserAgentContext;
import org.lobobrowser.html.parser.DocumentBuilderImpl;
import org.lobobrowser.html.parser.InputSourceImpl;
import org.lobobrowser.html.test.SimpleUserAgentContext;

import org.mightyfrog.util.xpathtester.Plugin;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author Shigehiro Soejima
 */
public class CobraPlugin implements Plugin {
    //
    private Charset charset = Charset.defaultCharset();

    {
        Logger.getLogger("org.lobobrowser").setLevel(Level.OFF);
    }

    /** */
    @Override
    public Document getDocument(URL url) throws Exception {
        Document doc = null;
        DocumentBuilderImpl builder =
            new DocumentBuilderImpl(new SimpleUserAgentContext());
        InputStream in = null;
        Reader reader = null;
        try {
            in = url.openStream();
            reader = new InputStreamReader(in, getCharset());
            InputSourceImpl is = new InputSourceImpl(reader, url.toString());
            doc = builder.parse(is);
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
        return "Cobra Plugin";
    }
}
