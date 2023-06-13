package br.ce.wcaquino.rest.tests;

import br.ce.wcaquino.rest.core.BaseTest;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.HashMap;
import java.util.Map;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BarrigaTest  extends BaseTest {

    private String TOKEN;

    private static String CONTA_NAME = "Conta " + System.nanoTime();
    private static Integer CONTA_ID;
    @Before
    public void login() {

        Map<String, String> login = new HashMap<>();
        login.put("email","martins@email.com");
        login.put("senha","123456");

        TOKEN = given().
                body(login).
                when().
                post("/signin").
                then().
                statusCode(200).
                extract().path("token")
                ;

    }

    @Test
    public void t01_naoDeveAcessarAPISemToken() {

        given()
                .when()
                    .get("/contas")
                .then()
                    .statusCode(401);
    }

    @Test
    public void t02_deveIncluirContaComSucesso() {
        Map< String, String> login = new HashMap<>();
        login.put( "email", "wagner@aquino" );
        login.put( "senha", "123456" );

        TOKEN = given()
                .body(login)
            .when()
                .post("/signin")
            .then()
                .statusCode(200 )
                .extract().path("token");

        CONTA_ID = given()
                .header("Authorization", "JWT " + TOKEN)
                .body( "{\"nome\": \""+CONTA_NAME+"\"}" )
            .when()
                .post("/contas")
            .then()
                .statusCode(200)
                .extract().path("id");
    }

    @Test
    public void t03_deveEditarContaComSucesso() {

        given().
                header("Authorization", "JWT " + TOKEN ).
                body("{ \"nome\": \"" +CONTA_NAME+ " alterado\" }")
                .pathParam("id", CONTA_ID).
            when().
                put("/contas/{id}").
            then().
                log().all().
                statusCode(200)
                .body("nome", Matchers.is(CONTA_NAME + " alterado"))
        ;
    }

    @Test
    public void t04_naoDeveInserirContaComSucesso() {

        given().
                header("Authorization", "JWT " + TOKEN ).
                body("{ \"nome\": \"conta qualquer23\" }").
            when().
                post("/contas").
            then().
                statusCode(400)
                .body("error", Matchers.is("Já existe uma conta com esse nome!"))
        ;
    }

    @Test
    public void t05_deveInserirMovimentacaoComSucesso() {

        Movimentacap mov = new Movimentacap();
        mov.setConta_id(CONTA_ID);
        //mov.setUsuario_id(usuario_id);
        mov.setDescricao( "Descrição da movimentação" );
        mov.setEnvolvido("Envolvido na mov");
        mov.setTipo( "REC" );
        mov.setData_transacao("01/01/2010");
        mov.setData_pagamento("10/05/2020");
        mov.setValor(100f);
        mov.setStatus(true);

        given().
                header("Authorization", "JWT " + TOKEN ).
                body(mov).
            when().
                post("/transacoes").
            then().
                log().all().
                statusCode(201)
        ;
    }

    @Test
    public void t06_deveValidarCamposObrigatoriosMovimentacao() {

        given().
                header("Authorization", "JWT " + TOKEN).
                body("{}").
            when().
                post("/transacoes").
            then().
                log().all().
                statusCode(400)
                .body("$", hasSize(8))
                .body("msg", hasItems(
                        "Data da Movimentação é obrigatório",
                        "Data do pagamento é obrigatório",
                        "Descrição é obrigatório",
                        "Interessado é obrigatório",
                        "Valor é obrigatório",
                        "Valor deve ser um número",
                        "Conta é obrigatório",
                        "Situação é obrigatório"
                ));
    }

    @Test
    public void t07_naoDeveInserirMovimentacaoComDataFutura() {
        Movimentacap mov = getMovimentacaoValida();
        mov.setData_transacao("20/05/2025");

        given().
                header("Authorization", "JWT " + TOKEN).
                body(mov).
        when().
                post("/transacoes").
        then().
                statusCode(400).
                body("$", hasSize(1)).
                body("msg", hasItem("Data da Movimentação deve ser menor ou igual à data atual"))
                ;

    }

    @Test
    public void t08_naoDeveRemoverContaComMovimentacaoComDataFutura() {
        Movimentacap mov = getMovimentacaoValida();
        mov.setData_transacao("20/05/2025");

        given().
                header("Authorization", "JWT " + TOKEN).
        when().
                delete("/contas/1757707").
        then().
                statusCode(500).
                body("constraint", is("transacoes_conta_id_foreign"))
        ;

    }

    @Test
    public void t09_deveCalcularSaldoContas() {

        given().
                header("Authorization", "JWT " + TOKEN).
            when().
                get("/saldo").
            then().
                statusCode(200).
                body("find{it.conta_id == 1757707}.saldo", is("300.00"))
                //body("constraint", is("transacoes_conta_id_foreign"))
        ;

    }

    @Test
    public void t10_deveRemoverMovimentacoes() {

        given().
                header("Authorization", "JWT " + TOKEN).
            when().
                delete("/transacoes/1657258").
            then().
                statusCode(204);

    }

    private Movimentacap getMovimentacaoValida() {

        Movimentacap mov = new Movimentacap();
        mov.setConta_id(1757707);
        mov.setDescricao("Descricao da movimentacao");
        mov.setEnvolvido("Envolvido na mov");
        mov.setTipo("REC");
        mov.setData_transacao("01/01/2000");
        mov.setData_pagamento("01/05/2010");
        mov.setValor(100f);
        mov.setStatus(true);
        return mov;

    }


}

