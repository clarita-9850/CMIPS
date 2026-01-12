package com.cmips.baw.destination;

import com.cmips.integration.framework.baw.destination.Destination;
import com.cmips.integration.framework.baw.destination.Sftp;

/**
 * SFTP destination configuration for State Controller Office (SCO).
 *
 * SCO receives payment request files (PRDR120A) that contain approved
 * timesheet payment requests for IHSS providers.
 *
 * Outbound files are uploaded to this destination after processing.
 */
@Destination(
    name = "sco-sftp",
    description = "State Controller Office - Payment Request Files (Outbound)"
)
@Sftp(
    host = "${baw.sftp.sco.host}",
    port = 22,
    remotePath = "${baw.sftp.sco.outbound-path}",
    credentials = "sco-sftp-creds",
    connectionTimeout = 30000,
    createDirectory = true,
    tempSuffix = ".tmp",
    knownHosts = "${baw.sftp.sco.known-hosts:}"
)
public interface ScoSftpDestination {
    // Marker interface - configuration is in annotations
}
