[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [ValidateSet('health', 'capabilities', 'status', 'query', 'command')]
    [string]$Action,
    [Parameter(Position = 1)]
    [ValidateSet('world.time', 'world.block', 'lunar.snapshot', 'lunar.events', 'inventory.player', 'inventory.container', 'machine.inspect',
        'test.context', 'fixture.create', 'fixture.reset', 'fixture.destroy', 'test.input', 'hopper.prepare_capacity', 'hopper.stage_reload',
        'hopper.power_on', 'hopper.power_off', 'distillation.create', 'distillation.start', 'distillation.start_partial',
        'distillation.collect', 'distillation.transport', 'crafting.transactions')]
    [string]$Op,
    [string]$Discovery = $env:ZTO_DEV_BRIDGE_DISCOVERY,
    [int]$Dimension = 0,
    [int]$X,
    [int]$Y,
    [int]$Z,
    [long]$WorldId,
    [long]$AfterSequence = 0,
    [long]$SessionId,
    [long]$RunId,
    [long]$Revision,
    [int]$Count,
    [string]$RequestId = [Guid]::NewGuid().ToString('N'),
    [switch]$Json,
    [switch]$Quiet
)

$ErrorActionPreference = 'Stop'
$failureCode = 'INVALID_ARGUMENT'
$failureMessage = 'Use the discovery path printed by the game and supply the requested query arguments.'
$client = $null
$request = $null
$response = $null

try {
    if (!$Discovery -or ($Action -in 'query', 'command' -and !$Op)) { throw 'Missing argument' }
    if ($Op -eq 'lunar.events' -and ($WorldId -le 0 -or $AfterSequence -lt 0)) { throw 'Invalid lunar cursor' }
    if ($Op -in 'world.block', 'inventory.container', 'machine.inspect') {
        foreach ($coordinate in 'X', 'Y', 'Z') {
            if (!$PSBoundParameters.ContainsKey($coordinate)) { throw 'Missing coordinate' }
        }
    }
    $session = Get-Content -LiteralPath $Discovery -Raw | ConvertFrom-Json
    if ($session.apiVersion -ne '1.0' -or ($session.port -isnot [int] -and $session.port -isnot [long]) -or $session.port -lt 1 -or $session.port -gt 65535 -or
        $session.token -cnotmatch '^[A-Za-z0-9_-]{43}$') { throw 'Invalid discovery file' }

    $failureCode = 'CONNECTION_FAILED'
    $failureMessage = 'The local bridge did not return a valid response. Check that the game is running.'
    Add-Type -AssemblyName System.Net.Http
    $handler = New-Object System.Net.Http.HttpClientHandler
    $handler.UseProxy = $false
    $handler.AllowAutoRedirect = $false
    $client = New-Object System.Net.Http.HttpClient($handler)
    $client.Timeout = [TimeSpan]::FromSeconds(3)
    $client.MaxResponseContentBufferSize = 8192
    $method = [System.Net.Http.HttpMethod]::Get
    if ($Action -in 'query', 'command') { $method = [System.Net.Http.HttpMethod]::Post }
    $endpoint = 'http://127.0.0.1:{0}/v1/{1}' -f $session.port, $Action.ToLowerInvariant()
    $request = New-Object System.Net.Http.HttpRequestMessage($method, $endpoint)
    $request.Headers.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue('Bearer', $session.token)
    if ($Action -in 'query', 'command') {
        $arguments = @{}
        if ($Op -in 'world.block', 'inventory.container', 'machine.inspect') { $arguments = @{ dimension = $Dimension; x = $X; y = $Y; z = $Z } }
        if ($Op -eq 'lunar.events') { $arguments = @{ worldId = $WorldId; afterSequence = $AfterSequence } }
        if ($Op -in 'inventory.container', 'machine.inspect') { $arguments.worldId = $WorldId }
        if ($Action -eq 'command') {
            $arguments = @{ worldId = $WorldId; sessionId = $SessionId; runId = $RunId; revision = $Revision }
            if ($Op -eq 'test.input') { $arguments.count = $Count }
        }
        $body = @{ requestId = $RequestId; op = $Op.ToLowerInvariant(); args = $arguments } | ConvertTo-Json -Compress
        $request.Content = New-Object System.Net.Http.StringContent($body, [Text.Encoding]::UTF8)
        $request.Content.Headers.ContentType = New-Object System.Net.Http.Headers.MediaTypeHeaderValue('application/json')
    }
    $response = $client.SendAsync($request).GetAwaiter().GetResult()
    $raw = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    $result = $raw | ConvertFrom-Json
    if ($result.apiVersion -ne '1.0' -or $result.ok -isnot [bool]) { throw 'Invalid bridge response' }
    if (!$Quiet) {
        if ($Json) { $raw }
        elseif ($result.ok) { $result.data | Format-List | Out-String | ForEach-Object { $_.Trim() } }
        else { '{0} - {1}' -f $result.error.code, $result.error.message }
    }
    if (!$result.ok) { exit 1 }
    exit 0
} catch {
    if (!$Quiet) {
        if ($Json) {
            @{ apiVersion = '1.0'; requestId = $null; ok = $false; error = @{
                code = $failureCode; message = $failureMessage
            } } | ConvertTo-Json -Compress
        } else {
            "$failureCode - $failureMessage"
        }
    }
    exit 2
} finally {
    if ($response) { $response.Dispose() }
    if ($request) { $request.Dispose() }
    if ($client) { $client.Dispose() }
}
