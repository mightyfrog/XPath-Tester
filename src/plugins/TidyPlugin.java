package plugins;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;

import org.mightyfrog.util.xpathtester.Plugin;

import org.w3c.dom.Document;
import org.w3c.tidy.Tidy;

/**
 *
 * @author Shigehiro Soejima
 */
public class TidyPlugin implements Plugin {
    //
    private Charset charset = Charset.defaultCharset();

    /** */
    @Override
    public Document getDocument(URL url) throws Exception {
        Document doc = null;
        InputStream in = null;
        try {
            Tidy tidy = new Tidy();
            tidy.setXHTML(true);
            tidy.setXmlOut(true);
            tidy.setQuiet(true);
            tidy.setInputEncoding(getCharset().toString());
            in = url.openStream();
            doc = tidy.parseDOM(in, null);
        } catch (IOException e) {
            throw e;
        } finally {
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
        return "Tidy HTML Plugin";
    }
}
