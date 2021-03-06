/*
 * Copyright 2012 Rui Araújo, Luís Fonseca
 *
 * This file is part of Router Keygen.
 *
 * Router Keygen is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Router Keygen is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Router Keygen.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.exobel.routerkeygen.algorithms;

import android.os.Parcel;
import android.os.Parcelable;

import org.exobel.routerkeygen.R;
import org.exobel.routerkeygen.config.AliceMagicInfo;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;

public class AliceItalyKeygen extends Keygen {

    public final static byte ALICE_SEED[/* 32 */] = {0x64, (byte) 0xC6,
            (byte) 0xDD, (byte) 0xE3, (byte) 0xE5, 0x79, (byte) 0xB6,
            (byte) 0xD9, (byte) 0x86, (byte) 0x96, (byte) 0x8D, 0x34, 0x45,
            (byte) 0xD2, 0x3B, 0x15, (byte) 0xCA, (byte) 0xAF, 0x12,
            (byte) 0x84, 0x02, (byte) 0xAC, 0x56, 0x00, 0x05, (byte) 0xCE,
            0x20, 0x75, (byte) 0x91, 0x3F, (byte) 0xDC, (byte) 0xE8};
    public static final Parcelable.Creator<AliceItalyKeygen> CREATOR = new Parcelable.Creator<AliceItalyKeygen>() {
        public AliceItalyKeygen createFromParcel(Parcel in) {
            return new AliceItalyKeygen(in);
        }

        public AliceItalyKeygen[] newArray(int size) {
            return new AliceItalyKeygen[size];
        }
    };
    final static private String preInitCharset = "0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvwxyz0123456789abcdefghijklmnopqrstuvWxyz0123";
    final private String ssidIdentifier;
    final private List<AliceMagicInfo> supportedAlice;

    public AliceItalyKeygen(String ssid, String mac,
                            List<AliceMagicInfo> supportedAlice) {
        super(ssid, mac);
        this.ssidIdentifier = ssid.substring(ssid.length() - 8);
        this.supportedAlice = supportedAlice;
    }

    @SuppressWarnings("unchecked")
    private AliceItalyKeygen(Parcel in) {
        super(in);
        ssidIdentifier = in.readString();
        supportedAlice = in
                .readArrayList(AliceMagicInfo.class.getClassLoader());
    }

    @Override
    public List<String> getKeys() {
        if (supportedAlice == null || supportedAlice.isEmpty()) {
            setErrorCode(R.string.msg_erralicenotsupported);
            return null;
        }
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e1) {
            setErrorCode(R.string.msg_nosha256);
            return null;
        }
        for (int j = 0; j < supportedAlice.size(); ++j) {/* For pre AGPF 4.5.0sx */
            String serialStr = supportedAlice.get(j).getSerial() + "X";
            int k = supportedAlice.get(j).getMagic()[0];
            int Q = supportedAlice.get(j).getMagic()[1];
            int serial = (Integer.valueOf(ssidIdentifier) - Q) / k;
            String tmp = Integer.toString(serial);
            for (int i = 0; i < 7 - tmp.length(); i++) {
                serialStr += "0";
            }
            serialStr += tmp;

            byte[] mac = new byte[6];
            String key = "";
            byte[] hash;

            if (getMacAddress().length() == 12) {

                for (int i = 0; i < 12; i += 2)
                    mac[i / 2] = (byte) ((Character.digit(getMacAddress()
                            .charAt(i), 16) << 4) + Character.digit(
                            getMacAddress().charAt(i + 1), 16));

                md.reset();
                md.update(ALICE_SEED);
                try {
                    md.update(serialStr.getBytes("ASCII"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                md.update(mac);
                hash = md.digest();
                for (int i = 0; i < 24; ++i) {
                    key += preInitCharset.charAt(hash[i] & 0xFF);
                }
                addPassword(key);
            }

			/* For post AGPF 4.5.0sx */
            String macEth = getMacAddress().substring(0, 6);

            for (int extraNumber = 0; extraNumber < 10; extraNumber++) {
                String calc = Integer.toHexString(
                        Integer.valueOf(extraNumber + ssidIdentifier))
                        .toUpperCase(Locale.getDefault());
                if (macEth.charAt(5) == calc.charAt(0)) {
                    macEth += calc.substring(1);
                    break;
                }
            }
            if (macEth.equals(getMacAddress().substring(0, 6))) {
                continue;
            }
            for (int i = 0; i < 12; i += 2)
                mac[i / 2] = (byte) ((Character.digit(macEth.charAt(i), 16) << 4) + Character
                        .digit(macEth.charAt(i + 1), 16));
            md.reset();
            md.update(ALICE_SEED);
            try {
                md.update(serialStr.getBytes("ASCII"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            md.update(mac);
            key = "";
            hash = md.digest();
            for (int i = 0; i < 24; ++i)
                key += preInitCharset.charAt(hash[i] & 0xFF);
            addPassword(key);
        }
        return getResults();
    }

    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(ssidIdentifier);
        dest.writeList(supportedAlice);
    }

}
