package world.crafty.pe.session.pass

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

interface EncryptionPass {
    fun encrypt(bytes: ByteArray) : ByteArray
    fun decrypt(bytes: ByteArray) : ByteArray
}

object NoEncryptionPass : EncryptionPass {
    override fun encrypt(bytes: ByteArray): ByteArray {
        return bytes
    }
    override fun decrypt(bytes: ByteArray): ByteArray {
        return bytes
    }
}

class MinecraftPeEncryptionPass(secretKey: SecretKeySpec, iv: ByteArray) : EncryptionPass {
    val cipher = createCipher(Cipher.ENCRYPT_MODE, secretKey, iv)
    val decipher = createCipher(Cipher.DECRYPT_MODE, secretKey, iv)
    
    override fun encrypt(bytes: ByteArray): ByteArray {
        throw NotImplementedError()
        return cipher.doFinal(bytes)
    }
    override fun decrypt(bytes: ByteArray): ByteArray {
        throw NotImplementedError()
        return decipher.doFinal(bytes)
    }

    private fun createCipher(mode: Int, secret: SecretKey, iv: ByteArray) : Cipher {
        val cipher = Cipher.getInstance("AES/CFB8/NoPadding")
        cipher.init(mode, secret, IvParameterSpec(iv))
        return cipher
    }

    /*
    fun sendEncrypted(packet: PePacket) {
        val byteStream = ByteArrayOutputStream()
        val mcStream = MinecraftOutputStream(byteStream)
        mcStream.writeByte(packet.id)
        packet.serialize(mcStream)
        val payload = byteStream.toByteArray()
        mcStream.write(createTrailer(payload, sendCounter.getAndIncrement(), secretKey))
        val payloadAndTrailer = byteStream.toByteArray()
        
        val encryptedPacket = cipher.doFinal(payloadAndTrailer)
        
        val wrapperByteStream = ByteArrayOutputStream()
        val wrapperMcStream = MinecraftOutputStream(wrapperByteStream)
        wrapperMcStream.writeByte(EncryptionWrapperPePacket.Codec.id)
        wrapperMcStream.write(encryptedPacket)
        
        ...
    }
    
    private fun createTrailer(payload: ByteArray, senderCounter: Long, secretKey: SecretKey) : ByteArray {
        val sha256 = MessageDigest.getInstance("SHA-256")
        sha256.update(senderCounter.toBytes())
        sha256.update(payload)
        sha256.update(secretKey.encoded)
        return sha256.digest()
    }
    */
}