/*
 * MediaFile.java
 *
 * Created on November 30, 2006, 4:14 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.panayotis.jubler.media;

import com.panayotis.jubler.Jubler;
import com.panayotis.jubler.media.player.AudioFileFilter;
import com.panayotis.jubler.media.player.VideoFileFilter;
import com.panayotis.jubler.media.preview.decoders.AudioCache;
import com.panayotis.jubler.os.FileCommunicator;
import java.io.File;

/**
 *
 * @author teras
 */
public class MediaFile {
    
    private String vfile;   /* Video file */
    private String afile;   /* Audio file - prossibly same as video file */
    private String cfile;   /* Cache file */
    
    /** Creates a new instance of MediaFile */
    public MediaFile() {
        vfile = "";
        afile = "";
        cfile = "";
    }
    
    public MediaFile(MediaFile m) {
        vfile = m.vfile;
        afile = m.afile;
        cfile = m.cfile;
    }
    
    public boolean equals(Object o) {
        if (o instanceof MediaFile) {
            MediaFile m = (MediaFile)o;
            return vfile.equals(m.vfile) && afile.equals(m.afile) && cfile.equals(m.cfile);
        }
        return super.equals(o);
    }
    
    public void guessFiles(String guess) {
        
        if (vfile.equals("")){
            vfile = FileCommunicator.guessFile(guess, new VideoFileFilter());
        }
        
        if (afile.equals("")) {
            setAudioFileUnused();
        }
        
        if (cfile.equals("")){
            /* Find the base name (without extension) of the selected "audio" file */
            int point = afile.lastIndexOf('.');
            if (point < 0 ) point = afile.length();
            
            /* Create a special filter for the selected video file */
            AudioFileFilter filter = new AudioFileFilter();
            filter.setCheckForValidCache(vfile);
            
            /* Set cfile */
            updateCacheFile(FileCommunicator.guessFile( afile.substring(0,point), filter));
        }
    }
    
    public String getVideoFile() {
        return vfile;
    }
    
    public String getAudioFile() {
        return afile;
    }
    
    public String getCacheFile() {
        return cfile;
    }
    
    
    public void setVideoFile(String vfname) {
        if (!fileExists(vfname)) return;
        if (isAudioFileUnused()) setAudioFile(vfname);
        
        /* This SHOULD come after audio check, or else it thinks that a different audio stream is required */
        vfile = vfname;
    }
    
    public void setAudioFile(String afname) {
        if (!fileExists(afname)) return;
        afile = afname;
        
        updateCacheFile(afname);
    }
    
    public void setCacheFile(String cfname) {
        if (cfname==null || cfname.equals("")) return;
        updateCacheFile(cfname);
        
        /* Set audio file, from the cache file */
        String audioname = AudioCache.getNameFromCache(cfile);
        if (audioname!=null && (!audioname.trim().equals(""))){
            File newafile = new File( (new File(cfname)).getParent(), audioname);
            if (newafile.exists()) afile = audioname;
        }
        
    }
    
    
    private void updateCacheFile(String cfname) {
        if (cfname==null || cfname.equals("")) return;
        
        // clean old cache file
        cleanUp();
        
        /* Find a write enabled cache file */
        File cf = new File(cfname);
        if (!( cf.getParentFile().canWrite() && ((!cf.exists()) || cf.canWrite()) )) {
            String strippedfilename = cf.getName();
            int point = strippedfilename.lastIndexOf('.');
            if (point < 0 ) point = strippedfilename.length();
            cf = new File(System.getProperties().getProperty("java.io.tmpdir")+
                    System.getProperties().getProperty("file.separator")+
                    strippedfilename.substring(0,point)+AudioCache.getExtension());
        } else {
            int point = cfname.lastIndexOf('.');
            if (point < 0 ) point = cfname.length();
            cf = new File( cfname.substring(0,point)+AudioCache.getExtension());
        }
        cfile = cf.getPath();
    }
    
    public void cleanUp() {
//        
//        File cachefile = new File(cfile);
//        if (cachefile.exists() && cachefile.canWrite())
//            cachefile.delete();
    }
    
    
    public boolean isValidMediaFile() {
        return (fileExists(vfile));
    }
    
    public boolean isAudioFileUnused() {
        return vfile.equals(afile);
    }
    
    public void setAudioFileUnused() {
        afile = vfile;
        updateCacheFile(vfile);
    }
    

    
    public float getFramesPerSecond(Jubler jub) {
       return jub.getSubPreview().getFPS();
    }
    
    
    private static final boolean fileExists(String fname) {
        if (fname==null || fname.equals("")) return false;
        return new File(fname).exists();
    }
    
    
}
