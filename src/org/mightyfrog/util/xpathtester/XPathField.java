package org.mightyfrog.util.xpathtester;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

/**
 *
 *
 */
class XPathField extends JTextField {
    //
    protected static final XPathFieldListener DF_LISTENER =
        new XPathFieldListener();

    /**
     *
     */
    public XPathField() {
        addMouseListener(DF_LISTENER);
    }

    /** */
    @Override
    public void setText(String text) {
        super.setText(text);
        setCaretPosition(0);
    }

    //
    //
    //

    /**
     *
     * @param evt
     */
    void showPopup(MouseEvent evt) {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem copyMI = new JMenuItem() {
                {
                    setText("Copy");
                    addActionListener(new ActionListener() {
                            /** */
                            @Override
                            public void actionPerformed(ActionEvent evt) {
                                copy();
                            }
                        });
                }
            };
        copyMI.setEnabled(getCaret().getDot() != getCaret().getMark());
        popup.add(copyMI);

        JMenuItem cutMI = new JMenuItem() {
                {
                    setText("Cut");
                    addActionListener(new ActionListener() {
                            /** */
                            @Override
                            public void actionPerformed(ActionEvent evt) {
                                cut();
                            }
                        });
                }
            };
        cutMI.setEnabled(getCaret().getDot() != getCaret().getMark());
        popup.add(cutMI);

        JMenuItem pasteMI = new JMenuItem() {
                {
                    setText("Paste");
                    addActionListener(new ActionListener() {
                            /** */
                            @Override
                            public void actionPerformed(ActionEvent evt) {
                                paste();
                            }
                        });
                }
            };
        popup.add(pasteMI);

        popup.show(this, evt.getX(), evt.getY());
    }

    //
    //
    //

    /**
     *
     */
    private static class XPathFieldListener implements MouseListener {
        /** */
        @Override
        public void mouseEntered(MouseEvent evt) {
            // no-op
        }

        /** */
        @Override
        public void mousePressed(MouseEvent evt) {
            if (evt.isPopupTrigger()) {
                XPathField df = (XPathField) evt.getSource();
                df.showPopup(evt);
            }
        }

        /** */
        @Override
        public void mouseClicked(MouseEvent evt) {
            // no-op
        }

        /** */
        @Override
        public void mouseReleased(MouseEvent evt) {
            if (evt.isPopupTrigger()) {
                XPathField df = (XPathField) evt.getSource();
                df.showPopup(evt);
            }
        }

        /** */
        @Override
        public void mouseExited(MouseEvent evt) {
            // no-op
        }
    }
}
