package com.cmips.baw.destination;

import com.cmips.integration.framework.baw.destination.Destination;
import com.cmips.integration.framework.baw.destination.Sftp;

/**
 * SFTP source configuration for Department of Justice (DOJ).
 *
 * DOJ sends background check result files for IHSS providers.
 *
 * Inbound files are downloaded from this source for processing.
 */
@Destination(
    name = "doj-sftp",
    description = "Department of Justice - Background Check Files (Inbound)"
)
@Sftp(
    host = "${baw.sftp.doj.host}",
    port = 22,
    remotePath = "${baw.sftp.doj.inbound-path}",
    credentials = "doj-sftp-creds",
    connectionTimeout = 30000,
    createDirectory = false,
    tempSuffix = "",
    knownHosts = "${baw.sftp.doj.known-hosts:}"
)
public interface DojSftpSource {
    // Marker interface - configuration is in annotations
}
