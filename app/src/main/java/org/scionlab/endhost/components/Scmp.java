/*
 * Copyright (C) 2019-2020 Vera Clemens, Tom Kranz, Tom Heimbrodt, Elias Kuiter
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.scionlab.endhost.components;

import android.content.Context;

import org.scionlab.endhost.Logger;
import org.scionlab.endhost.ScionBinary;
import org.scionlab.endhost.ScionComponent;
import org.scionlab.endhost.ScionComponentRegistry;
import org.scionlab.endhost.Config;

public class Scmp extends ScionComponent {
    private static final String TAG = "Scmp";

    public Scmp(Context context) {
        super(context);
    }

    @Override
    public void prepare() {
        /*File gencert = mkdir("gen-certs");
        Provider bcProvider = new BouncyCastleProvider();
        Security.addProvider(bcProvider);
        File key = new File(gencert, "tls.key");
        File cert = new File(gencert, "tls.pem");
        KeyPair kp = null;
        JcaPEMWriter pWriter;
        if (!key.exists()) {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", bcProvider);
            kpg.initialize(2048);
            kp = kpg.generateKeyPair();
            pWriter = new JcaPEMWriter(new PrintWriter(key));
            pWriter.writeObject(kp);
            pWriter.close();
            log(R.string.scmpcreatekey, key.getAbsolutePath());
        } else if (!cert.exists()) {
            PEMParser p = new PEMParser(new FileReader(key));
            JcaPEMKeyConverter conv = new JcaPEMKeyConverter();
            kp = conv.getKeyPair((PEMKeyPair) p.readObject());
            log(R.string.scmpreadkey, key.getAbsolutePath());
        } else {
            log(R.string.scmpcertexists, key.getAbsolutePath(), cert.getAbsolutePath());
        }
        if (!cert.exists() && kp != null) {
            ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(kp.getPrivate());
            JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                    new X500Name("CN=scion_def_srv"),
                    new BigInteger(160, new SecureRandom()),
                    new Date(),
                    new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365 * 10),
                    new X500Name("CN=scion_def_srv"),
                    kp.getPublic()
            );
            certBuilder.addExtension(new ASN1ObjectIdentifier("2.5.29.19"), true, new BasicConstraints(true));
            pWriter = new JcaPEMWriter(new PrintWriter(cert));
            pWriter.writeObject(new JcaX509CertificateConverter().setProvider(bcProvider).getCertificate(certBuilder.build(signer)));
            pWriter.close();
            log(R.string.scmpcreatecert, cert.getAbsolutePath());
        }*/
    }

    @Override
    public boolean mayRun() {
        return ScionComponentRegistry.getInstance().isReady(Dispatcher.class, Daemon.class);
    }

    @Override
    public void run() {
        ScionBinary.runScmp(context,
                Logger.createLogThread(TAG),
                storage.getAbsolutePath(Config.Dispatcher.SOCKET_PATH),
                storage.getAbsolutePath(Config.Daemon.RELIABLE_SOCKET_PATH),
                "17-ffaa:1:cf9,[192.168.0.123]",
                "19-ffaa:0:1301,[0.0.0.0]"); // 17-ffaa:1:cf9,[192.168.0.8]
    }
}
