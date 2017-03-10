package world.crafty.pc

import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher

private val rand = SecureRandom()

fun generateKeyPair() : KeyPair {
    val gen = KeyPairGenerator.getInstance("RSA")
    gen.initialize(1024)
    return gen.genKeyPair()
}

fun generateVerifyToken() : ByteArray {
    val token = ByteArray(4)
    rand.nextBytes(token)
    return token
}

fun convertKeyToX509(key: Key) : Key {
    val spec = X509EncodedKeySpec(key.encoded)
    val factory = KeyFactory.getInstance("RSA")
    return factory.generatePublic(spec)
}

fun convertKeyToX509(key: ByteArray) : Key {
    val spec = X509EncodedKeySpec(key)
    val factory = KeyFactory.getInstance("RSA")
    return factory.generatePublic(spec)
}

fun createDecipher(key: KeyPair) : Cipher {
    val decipher = Cipher.getInstance("RSA")
    decipher.init(Cipher.DECRYPT_MODE, key.private)
    return decipher
}