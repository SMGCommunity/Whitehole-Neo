package com.thesuncat.whitehole.swing;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.*;
import java.io.*;

public class CopyImage implements ClipboardOwner {
    
    public void copyImage(BufferedImage bi)
    {
        TransferableImage trans = new TransferableImage(bi);
        Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
        c.setContents( trans, this );
    }

    @Override
    public void lostOwnership( Clipboard clip, Transferable trans ) {
        clip.setContents(trans, null);
        System.out.println( "Lost clipboard Ownership??" );
    }
    private class TransferableImage implements Transferable {

        Image i;

        public TransferableImage( Image i ) {
            this.i = i;
        }

        @Override
        public Object getTransferData( DataFlavor flavor )
        throws UnsupportedFlavorException, IOException {
            if ( flavor.equals( DataFlavor.imageFlavor ) && i != null ) {
                return i;
            }
            else {
                throw new UnsupportedFlavorException( flavor );
            }
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            DataFlavor[] flavors = new DataFlavor[ 1 ];
            flavors[ 0 ] = DataFlavor.imageFlavor;
            return flavors;
        }

        @Override
        public boolean isDataFlavorSupported( DataFlavor flavor ) {
            DataFlavor[] flavors = getTransferDataFlavors();
            for (DataFlavor flavor1 : flavors) {
                if (flavor.equals(flavor1)) {
                    return true;
                }
            }

            return false;
        }
    }
}
