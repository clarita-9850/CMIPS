package com.cmips.baw.destination;

import com.cmips.integration.framework.baw.destination.Destination;
import com.cmips.integration.framework.baw.destination.Sftp;

/**
 * SFTP source configuration for State Treasurer Office (STO).
 *
 * STO sends daily warrant paid files (PRDR110A) that contain information
 * about warrants that have been paid, voided, or become stale.
 *
 * Inbound files are downloaded from this source for processing.
 */
@Destination(
    name = "sto-sftp",
    description = "State Treasurer Office - Warrant Paid Files (Inbound)"
)
@Sftp(
    host = "${baw.sftp.sto.host}",
    port = 22,
    remotePath = "${baw.sftp.sto.inbound-path}",
    credentials = "sto-sftp-creds",
    connectionTimeout = 30000,
    createDirectory = false,
    tempSuffix = "",
    knownHosts = "${baw.sftp.sto.known-hosts:}"
)
public interface StoSftpSource {
    // Marker interface - configuration is in annotations
}
