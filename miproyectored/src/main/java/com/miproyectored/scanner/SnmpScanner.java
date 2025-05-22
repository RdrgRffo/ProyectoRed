package com.miproyectored.scanner;

import org.snmp4j.CommunityTarget;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.PDU;
import com.miproyectored.model.Device;

public class SnmpScanner {
    private static final String COMMUNITY = "public";
    private static final int TIMEOUT = 1000;
    private static final int RETRIES = 2;
    
    public void enrichDeviceInfo(Device device) {
        try {
            Address targetAddress = GenericAddress.parse("udp:" + device.getIp() + "/161");
            CommunityTarget target = new CommunityTarget();
            target.setCommunity(new OctetString(COMMUNITY));
            target.setAddress(targetAddress);
            target.setRetries(RETRIES);
            target.setTimeout(TIMEOUT);
            target.setVersion(org.snmp4j.mp.SnmpConstants.version2c);

            TransportMapping<?> transport = new DefaultUdpTransportMapping();
            Snmp snmp = new Snmp(transport);
            transport.listen();

            // OIDs comunes para información del sistema
            String[] oids = {
                ".1.3.6.1.2.1.1.1.0",  // Descripción del sistema
                ".1.3.6.1.2.1.1.3.0",  // Tiempo de actividad
                ".1.3.6.1.2.1.25.2.3.1.5",  // Memoria total
                ".1.3.6.1.2.1.25.2.3.1.6"   // Memoria usada
            };

            PDU pdu = new PDU();
            for (String oid : oids) {
                pdu.add(new VariableBinding(new OID(oid)));
            }
            pdu.setType(PDU.GET);

            // Realizar la consulta SNMP
            org.snmp4j.event.ResponseEvent response = snmp.get(pdu, target);
            
            if (response != null && response.getResponse() != null) {
                PDU responsePDU = response.getResponse();
                // Procesar y añadir la información al objeto Device
                device.addSnmpInfo("systemInfo", responsePDU.getVariable(new OID(".1.3.6.1.2.1.1.1.0")).toString());
                device.addSnmpInfo("uptime", responsePDU.getVariable(new OID(".1.3.6.1.2.1.1.3.0")).toString());
                // ... procesar más información según necesites
            }

            transport.close();
        } catch (Exception e) {
            System.err.println("Error al obtener información SNMP para " + device.getIp() + ": " + e.getMessage());
        }
    }
}