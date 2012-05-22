/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.vfs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.util.IoUtil;
import com.caucho.vfs.GoogleInode.FileType;
import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileReadChannel;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;

/**
 * FilePath implements the native filesystem.
 */
abstract public class GooglePath extends FilesystemPath {
  private static Logger log = Logger.getLogger(GooglePath.class.getName());

  protected static final String QUERCUS_ROOT_PATH = "caucho-quercus-root";

  protected FileService _fileService;

  protected GooglePath _parent;
  protected GoogleInode _inode;

  /**
   * @param path canonical path
   */
  protected GooglePath(FilesystemPath root, String userPath, String path)
  {
    super(root, userPath, path);

    _separatorChar = getFileSeparatorChar();

    if (root != null) {
      GooglePath gsRoot = (GooglePath) root;

      _fileService = gsRoot._fileService;
    }
  }

  protected GooglePath()
  {
    super(null, "/", "/");

    _root = this;

    _fileService = FileServiceFactory.getFileService();

    _inode = new GoogleInode("", FileType.DIRECTORY, 0, 0);
  }

  protected void init()
  {
    if (readDirMap() == null) {
      _inode.setDirMap(new HashMap<String,GoogleInode>());

      writeDir(_inode.getDirMap());
    }
  }

  /**
   * Lookup the actual path relative to the filesystem root.
   *
   * @param userPath the user's path to lookup()
   * @param attributes the user's attributes to lookup()
   * @param path the normalized path
   *
   * @return the selected path
   */
  @Override
  public Path fsWalk(String userPath,
                     Map<String,Object> attributes,
                     String path)
  {
    if ("".equals(path) || "/".equals(path)) {
      return _root;
    }
    else {
      return createInstance(_root, userPath, path);
    }
  }

  abstract protected GooglePath createInstance(FilesystemPath root,
                                               String userPath,
                                               String path);

  @Override
  public GooglePath getParent()
  {
    if (_parent == null) {
      _parent = (GooglePath) super.getParent();
    }

    return _parent;
  }

  private boolean isRoot()
  {
    return getTail().equals("");
  }

  /**
   * Returns true if the path itself is cacheable
   */
  @Override
  protected boolean isPathCacheable()
  {
    return true;
  }

  @Override
  public String getScheme()
  {
    return "file";
  }

  /**
   * Returns the full url for the given path.
   */
  @Override
  public String getURL()
  {
    return escapeURL(getScheme() + ":" + getFullPath());
  }

  @Override
  public boolean exists()
  {
    GoogleInode inode = getGoogleInode();

    return inode != null && inode.exists();
  }

  @Override
  public int getMode()
  {
    int perms = 0;

    if (isDirectory()) {
      perms += 01000;
      perms += 0111;
    }

    if (canRead())
      perms += 0444;

    if (canWrite())
      perms += 0220;

    return perms;
  }

  @Override
  public boolean isExecutable()
  {
    return true;
  }

  @Override
  public boolean isDirectory()
  {
    GoogleInode inode = getGoogleInode();

    return inode != null && inode.isDirectory();
  }

  @Override
  public boolean isFile()
  {
    GoogleInode inode = getGoogleInode();

    return inode != null && inode.isFile();
  }

  @Override
  public long getLength()
  {
    GoogleInode inode = getGoogleInode();

    return inode != null ? inode.getLength() : -1;
  }

  @Override
  public long getLastModified()
  {
    GoogleInode inode = getGoogleInode();

    if (inode == null) {
      return -1;
    }

    return inode.getLastModified();
  }

  @Override
  public void setLastModified(long time)
  {
    GoogleInode inode = getGoogleInode();

    inode.setLastModified(time);

    writeInode(inode);
  }

  @Override
  public boolean canRead()
  {
    return exists();
  }

  @Override
  public boolean canWrite()
  {
    return exists();
  }

  /**
   * Returns a list of files in the directory.
   */
  @Override
  public String []list() throws IOException
  {
    HashMap<String,GoogleInode> dir = getDir();

    if (dir == null) {
      return new String[0];
    }

    ArrayList<String> names = new ArrayList<String>();

    for (GoogleInode inode : dir.values()) {
      if (inode.exists()) {
        names.add(inode.getName());
      }
    }

    String []list = new String[names.size()];
    names.toArray(list);

    Arrays.sort(list);

    return list;
  }

  @Override
  public boolean mkdir()
    throws IOException
  {
    // System.out.println("XX-MKDIR: " + getFullPath());

    if (exists()) {
      return false;
    }

    Path parent = getParent();

    if (parent.isFile()) {
      return false;
    }

    GoogleInode inode
      = new GoogleInode(getTail(), FileType.DIRECTORY, 0, 0);

    writeInode(inode);

    return true;
  }

  @Override
  public boolean mkdirs()
    throws IOException
  {
    if (exists()) {
      return false;
    }

    Path parent = getParent();

    parent.mkdirs();

    if (! parent.isDirectory()) {
      return false;
    }

    return mkdir();
  }

  @Override
  public boolean remove()
  {
    // System.out.println("XX-REMOVE: " + getFullPath());
    /*
    if (getFullPath().equals("/wp-content/pandora.tmp"))
      return false;
      */

    if (! exists()) {
      return false;
    }
    else if (isDirectory()) {
      try {
        if (list().length > 0) {
          return false;
        }
      } catch (IOException e) {
        log.log(Level.FINER, e.toString(), e);

        return false;
      }
    }

    GoogleInode inode = new GoogleInode(getTail(), FileType.NONE, -1, -1);
    // System.out.println("XX-REMOVE-OK: " + getFullPath());

    getParent().updateDir(inode);

    removeImpl();

    return true;
  }

  abstract protected boolean removeImpl();

  /*
  @Override
  public boolean truncate(long length)
    throws IOException
  {
    File file = getFile();

    clearStatusCache();

    FileOutputStream fos = new FileOutputStream(file);

    try {
      fos.getChannel().truncate(length);

      return true;
    } finally {
      fos.close();
    }
  }
  */

  @Override
  public boolean renameTo(Path path) throws IOException
  {
    ReadStream is = null;
    WriteStream os = null;

    boolean isSuccessful = false;

    TempBuffer tempBuffer = TempBuffer.allocate();

    try {
      is = openRead();
      os = path.openWrite();

      byte[] buffer = tempBuffer.getBuffer();

      while (true) {
        int readLen = is.read(buffer, 0, buffer.length);

        if (readLen < 0) {
          break;
        }

        os.write(buffer, 0, readLen);
      }

      isSuccessful = true;
      return true;
    }
    finally {
      TempBuffer.free(tempBuffer);

      IoUtil.close(is);
      IoUtil.close(os);

      if (isSuccessful) {
        remove();
      }
      else {
        path.remove();
      }
    }
  }

  /**
   * Returns the stream implementation for a read stream.
   */
  @Override
  public StreamImpl openReadImpl() throws IOException
  {
    try {
      if (! isRoot()) {
        GoogleInode inode = getParent().getGsInode(getTail());

        if (inode == null || ! inode.exists()) {
          // php/8134
          throw new FileNotFoundException(getFullPath());
        }
      }

      AppEngineFile file = getAppEngineFile();

      boolean isLock = false;
      FileReadChannel is = _fileService.openReadChannel(file, isLock);

      return new GoogleReadStream(this, is);
    } catch (FileNotFoundException e) {
      throw e;
    } catch (IOException e) {
      FileNotFoundException e1 = new FileNotFoundException(getURL() + ": " + e);
      e1.initCause(e);

      throw e1;
    }
  }

  @Override
  public RandomAccessStream openFileRandomAccess() throws IOException
  {
    // System.out.println("XX-RANDOM_ACCESS: " + getFullPath());
    return new GoogleRandomAccessStream(this, openWriteImpl());
  }

  @Override
  protected Path copy()
  {
    return createInstance(getRoot(), getUserPath(), getPath());
  }

  private GoogleInode getGsInode(String name)
  {
    HashMap<String,GoogleInode> dirMap = getDir();

    GoogleInode gsInode = null;
    if (dirMap != null) {
      gsInode = dirMap.get(name);
    }

    if (gsInode == null) {
      gsInode = new GoogleInode(name, FileType.NONE, -1, -1);
    }

    return gsInode;
  }

  private HashMap<String,GoogleInode> getDir()
  {
    if (! isDirectory()) {
      return null;
    }

    GoogleInode inode = getGoogleInode();

    if (inode.getDirMap() == null) {
      inode.setDirMap(readDirMap());
    }

    return inode.getDirMap();
  }

  HashMap<String,GoogleInode> readDirMap()
  {
    ReadStream is = null;

    try {
      is = openRead();

      Hessian2Input hIn = new Hessian2Input(is);

      HashMap map = (HashMap) hIn.readObject();

      hIn.close();

      return map;
    } catch (FileNotFoundException e) {
      log.log(Level.FINER, e.toString(), e);

      return null;
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);

      return null;
    } finally {
      IoUtil.close(is);
    }
  }

  void updateDir(GoogleInode inode)
  {
    // System.out.println("UPDATE: " + inode + " " + inode.getLength());
    HashMap<String,GoogleInode> map = readDirMap();

    if (map == null) {
      map = new HashMap<String,GoogleInode>();
    }

    map.put(inode.getName(), inode);

    GoogleInode dirInode = getGoogleInode();

    if (dirInode == null || ! dirInode.exists() || ! dirInode.isDirectory()) {
      dirInode = new GoogleInode(getTail(), FileType.DIRECTORY, 0, 0);

      if (isRoot()) {
        setGoogleInode(dirInode);
      }
      else {
        writeInode(dirInode);
      }
    }

    dirInode.setDirMap(map);

    writeDir(map);
  }

  void writeDir(HashMap<String,GoogleInode> map)
  {
    WriteStream out = null;
    try {
      out = openWrite();

      Hessian2Output hOut = new Hessian2Output(out);

      hOut.writeObject(map);

      hOut.close();
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    } finally {
      IoUtil.close(out);
    }
  }

  void setGoogleInode(GoogleInode inode)
  {
    _inode = inode;
  }

  void writeInode(GoogleInode inode)
  {
    clearStatusCache();

    GoogleInode oldInode = getGoogleInode();

    if (isRoot()) {
      setGoogleInode(inode);
    }
    else if (oldInode == null
             || ! oldInode.isDirectory()
             || ! getParent().isDirectory()) {
      GooglePath parent = (GooglePath) getParent();

      parent.updateDir(inode);
    }
  }

  GoogleInode getGoogleInode()
  {
    if (_inode != null) {
      return _inode;
    }
    else {
      return getParent().getGsInode(getTail());
    }
  }

  abstract AppEngineFile getAppEngineFile();

  @Override
  public int hashCode()
  {
    return getFullPath().hashCode();
  }

  @Override
  public boolean equals(Object b)
  {
    if (this == b)
      return true;

    if (! (b instanceof GooglePath))
      return false;

    GooglePath file = (GooglePath) b;

    return getFullPath().equals(file.getFullPath());
  }

  @Override
  public String toString()
  {
    return getURL();
  }
}
