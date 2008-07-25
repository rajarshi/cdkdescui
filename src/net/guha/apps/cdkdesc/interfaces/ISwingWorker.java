package net.guha.apps.cdkdesc.interfaces;

import net.guha.apps.cdkdesc.ExceptionInfo;

import java.util.List;

/**
 * @cdk.author Rajarshi Guha
 * @cdk.svnrev $Revision: 9162 $
 */
public interface ISwingWorker {
    void go();

    List<ExceptionInfo> getExceptionList();

    String getInputFormat();

    int getLengthOfTask();

    int getCurrent();

    void stop();

    boolean isDone();

    boolean isCancelled();
}
