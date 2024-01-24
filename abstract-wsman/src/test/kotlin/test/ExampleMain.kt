package test

import kr.jclab.wsman.abstractwsman.client.ClientHandler
import kr.jclab.wsman.abstractwsman.client.ClientRequestContext
import kr.jclab.wsman.abstractwsman.client.WsmanClient
import kr.jclab.wsman.abstractwsman.frame.SimpleResponseFrame
import org.apache.cxf.BusFactory
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse
import org.apache.hc.client5.http.impl.async.HttpAsyncClients
import org.apache.hc.core5.concurrent.FutureCallback
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.Method
import org.xmlsoap.schemas.ws._2004._09.enumeration.DataSource
import org.xmlsoap.schemas.ws._2004._09.enumeration.Enumerate
import org.xmlsoap.schemas.ws._2004._09.enumeration.EnumerationContextType
import org.xmlsoap.schemas.ws._2004._09.enumeration.Pull
import java.io.Closeable
import java.lang.Exception
import java.net.URI
import java.nio.charset.StandardCharsets

class SimpleClientHandler(
    private val uri: URI,
) : ClientHandler, Closeable {
    val httpClient = HttpAsyncClients.createDefault()

    override fun request(requestContext: ClientRequestContext, body: ByteArray) {
        val request = SimpleHttpRequest.create(
            Method.POST,
            uri
        )
        request.addHeader("Authorization", "Basic d3NtYW46c2VjcmV0")
        request.setBody(body, ContentType.APPLICATION_SOAP_XML.withCharset(StandardCharsets.UTF_8))
        httpClient.execute(request, object: FutureCallback<SimpleHttpResponse> {
            override fun completed(result: SimpleHttpResponse) {
                requestContext.emitResponseFrame(SimpleResponseFrame(result.code, result.bodyBytes))
            }

            override fun failed(ex: Exception) {
                requestContext.emitResponseException(ex)
            }

            override fun cancelled() {
                requestContext.emitResponseException(Exception("cancelled"))
            }
        })
    }

    override fun close() {
        httpClient.close()
    }
}

fun main() {
    val bus = BusFactory.getDefaultBus()
    val targetUri = URI.create("http://127.0.0.1:5600/wsman")
    SimpleClientHandler(targetUri).use { simpleClientHandler ->
        simpleClientHandler.httpClient.start()

        val wsmanClient = WsmanClient(bus, simpleClientHandler, targetUri.toString())
        val enumerator = wsmanClient.createResource("http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/CIM_System", DataSource::class.java)

        val enumResult = enumerator.enumerateOp(Enumerate())
        println("enumResult : ${enumResult}")

        val pullResult = enumerator.pullOp(Pull().also { pull ->
            pull.enumerationContext = EnumerationContextType().also {
                it.content.addAll(enumResult.enumerationContext.content)
            }
        })
        println("result : ${pullResult}")
    }
}