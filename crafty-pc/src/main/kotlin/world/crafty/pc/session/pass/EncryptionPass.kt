package world.crafty.pc.session.pass

import world.crafty.common.serialization.MinecraftOutputStream
import java.io.OutputStream
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

interface EncryptionPass {
    fun encryptionStream(stream: OutputStream) : MinecraftOutputStream
    fun decrypt(bytes: ByteArray) : ByteArray
}

object NoEncryptionPass : EncryptionPass {
    override fun encryptionStream(stream: OutputStream): MinecraftOutputStream {
        return MinecraftOutputStream(stream)
    }

    override fun decrypt(bytes: ByteArray): ByteArray {
        return bytes
    }
}

class MinecraftPcEncryptionPass(sharedSecret: SecretKeySpec) : EncryptionPass {
    private val cipher = createCipher(Cipher.ENCRYPT_MODE, sharedSecret)
    private val decipher = createCipher(Cipher.DECRYPT_MODE, sharedSecret)

    override fun encryptionStream(stream: OutputStream) = MinecraftOutputStream(CipherOutputStream(stream, cipher))

    override fun decrypt(bytes: ByteArray): ByteArray = decipher.update(bytes)
    
    private fun createCipher(mode: Int, secret: SecretKey) : Cipher {
        val cipher = Cipher.getInstance("AES/CFB8/NoPadding")
        cipher.init(mode, secret, IvParameterSpec(secret.encoded))
        return cipher
    }
}