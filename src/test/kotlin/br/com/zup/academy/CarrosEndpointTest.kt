package br.com.zup.academy

import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.inject.Singleton

@MicronautTest(transactional = false) // Devemos desativar o transactional pois o servidor gRPC roda em thread separada.
internal class CarrosEndpointTest(
    val repository: CarroRepository,
    val grpcClient: CarrosTestesGrpcServiceGrpc.CarrosTestesGrpcServiceBlockingStub
) {

    @BeforeEach
    fun setup() {
        repository.deleteAll()
    }

    @Test
    fun `deve adicionar um novo carro`() {
        //cenário

        //ação
        val request = CarroRequest.newBuilder()
            .setModelo("Gol")
            .setPlaca("ABC-1234")
            .build()
        val response = grpcClient.adicionar(request)

        //validação
        with(response) {
            assertNotNull(id)
            assertTrue(repository.existsById(id))
        }

    }

    @Test
    fun `nao deve adicionar um novo carro existente`() {
        //cenário
        val carroExistente = repository.save(
            Carro(
                modelo = "Gol",
                placa = "ABC-1234"
            )
        )

        //ação
        val error = assertThrows<StatusRuntimeException> {
            val request = CarroRequest.newBuilder()
                .setModelo("Palio")
                .setPlaca(carroExistente.placa)
                .build()
            grpcClient.adicionar(request)
        }

        //validação
        with(error) {
            assertEquals(Status.ALREADY_EXISTS.code, this.status.code)
            assertEquals("carro com placa existente", this.status.description)
        }
    }

    @Test
    fun `nao deve adicionar um novo carro com dados invalidos`() {
        //cenário

        //ação
        val error = assertThrows<StatusRuntimeException> {
            val request = CarroRequest.newBuilder()
                .setModelo("")
                .setPlaca("")
                .build()
            grpcClient.adicionar(request)
        }

        //validação
        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, this.status.code)
            assertEquals("dados de entrada inválidos", this.status.description)
        }
    }

    @Factory
    class Clients {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel):
                CarrosTestesGrpcServiceGrpc.CarrosTestesGrpcServiceBlockingStub? {
            return CarrosTestesGrpcServiceGrpc.newBlockingStub(channel)
        }
    }
}