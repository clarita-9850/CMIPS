package com.cmips.baw.destination;

import com.cmips.integration.framework.baw.destination.Destination;
import com.cmips.integration.framework.baw.destination.Sftp;

/**
 * SFTP source configuration for Employment Development Department (EDD).
 *
 * EDD sends benefits and tax withholding files for IHSS providers.
 *
 * Inbound files are downloaded from this source for processing.
 */
@Destination(
    name = "edd-sftp",
    description = "Employment Development Department - Benefits & Tax Files (Inbound)"
)
@Sftp(
    host = "${baw.sftp.edd.host}",
    port = 22,
    remotePath = "${baw.sftp.edd.inbound-path}",
    credentials = "edd-sftp-creds",
    connectionTimeout = 30000,
    createDirectory = false,
    tempSuffix = "",
    knownHosts = "${baw.sftp.edd.known-hosts:}"
)
public interface EddSftpSource {
    // Marker interface - configuration is in annotations
}
